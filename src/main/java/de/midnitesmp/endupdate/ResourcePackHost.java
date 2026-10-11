package de.midnitesmp.endupdate;

import com.sun.net.httpserver.HttpServer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public final class ResourcePackHost implements Listener, AutoCloseable {
    private final EndUpdatePlugin plugin;
    private final UUID packId = UUID.nameUUIDFromBytes("endupdate:resources".getBytes(StandardCharsets.UTF_8));
    private HttpServer server; private ExecutorService executor;
    private byte[] pack, hash; private String path; private int port;
    private final Map<UUID,String> statuses = new HashMap<>();
    public ResourcePackHost(EndUpdatePlugin plugin){this.plugin=plugin;}
    public void start() throws Exception {
        if(!plugin.getConfig().getBoolean("resource-pack.enabled",true))return;
        try(InputStream in=Objects.requireNonNull(plugin.getResource("endupdate-pack.zip"),"Embedded resource pack missing")){pack=in.readAllBytes();}
        hash=MessageDigest.getInstance("SHA-1").digest(pack);
        path="/endupdate/"+HexFormat.of().formatHex(hash)+".zip";
        Files.write(plugin.getDataFolder().toPath().resolve("endupdate-pack.zip"),pack);
        port=plugin.getConfig().getInt("resource-pack.port",0);
        if(port==0)port=Bukkit.getPort()+1;
        if(port<1||port>65535)throw new IllegalArgumentException("Resource-pack port must be 1–65535");
        server=HttpServer.create(new InetSocketAddress(plugin.getConfig().getString("resource-pack.bind-address","0.0.0.0"),port),32);
        executor=new ThreadPoolExecutor(2,4,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),r->{Thread t=new Thread(r,"EndUpdate-pack");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
        server.setExecutor(executor);
        server.createContext("/",exchange->{
            try(exchange){
                if(!exchange.getRequestURI().getPath().equals(path)){exchange.sendResponseHeaders(404,-1);return;}
                String method=exchange.getRequestMethod();
                if(!method.equals("GET")&&!method.equals("HEAD")){exchange.getResponseHeaders().set("Allow","GET, HEAD");exchange.sendResponseHeaders(405,-1);return;}
                exchange.getResponseHeaders().set("Content-Type","application/zip");
                exchange.getResponseHeaders().set("Cache-Control","public, max-age=31536000, immutable");
                exchange.getResponseHeaders().set("ETag",'"'+HexFormat.of().formatHex(hash)+'"');
                if(method.equals("HEAD")){exchange.getResponseHeaders().set("Content-Length",String.valueOf(pack.length));exchange.sendResponseHeaders(200,-1);}
                else {exchange.sendResponseHeaders(200,pack.length);exchange.getResponseBody().write(pack);}
            }catch(IOException ignored){ /* disconnected client */ }
        });
        server.start();plugin.getLogger().info("Embedded resource pack: TCP "+port+path+" (this port must be reachable by players).");
    }
    public boolean ready(){return server!=null;}
    public String localUrl(){return ready()?"http://127.0.0.1:"+port+path:"disabled";}
    public String url(Player p){
        String configured=plugin.getConfig().getString("resource-pack.public-url","").trim();
        if(!configured.isEmpty())return configured;
        InetSocketAddress address=p.getConnection().getVirtualHost();
        if(address==null)throw new IllegalStateException("No virtual host; configure resource-pack.public-url");
        try{return new URI("http",null,address.getHostString(),port,path,null,null).toASCIIString();}
        catch(URISyntaxException ex){throw new IllegalArgumentException("Invalid server hostname",ex);}
    }
    public void offer(Player p){
        if(!ready()){p.sendMessage("§eDas EndUpdate-Ressourcenpaket ist nicht verfuegbar; siehe Serverlog.");return;}
        try{
            p.addResourcePack(packId,url(p),hash,"EndUpdate: Mithryl-Texturen und Ruestung",plugin.getConfig().getBoolean("resource-pack.required",false));
            statuses.put(p.getUniqueId(),"angeboten");
        }catch(RuntimeException ex){plugin.getLogger().warning("Pack URL for "+p.getName()+": "+ex.getMessage());}
    }
    @EventHandler public void join(PlayerJoinEvent e){if(ready())Bukkit.getScheduler().runTaskLater(plugin,()->{if(e.getPlayer().isOnline())offer(e.getPlayer());},30);}
    @EventHandler public void status(PlayerResourcePackStatusEvent e){
        if(!e.getID().equals(packId))return;
        statuses.put(e.getPlayer().getUniqueId(),e.getStatus().name());
        switch(e.getStatus()){
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD -> {e.getPlayer().sendMessage("§eMithryl-Texturen konnten nicht geladen werden. Erneut versuchen: /endupdate pack");plugin.getLogger().warning("Resource pack "+e.getStatus()+" for "+e.getPlayer().getName()+". Check public URL / TCP "+port+".");}
            default -> {}
        }
    }
    @EventHandler public void quit(PlayerQuitEvent e){statuses.remove(e.getPlayer().getUniqueId());}
    public String status(Player p){return statuses.getOrDefault(p.getUniqueId(),"noch nicht angeboten");}
    @Override public void close(){if(server!=null){server.stop(0);server=null;}if(executor!=null)executor.shutdownNow();}
}

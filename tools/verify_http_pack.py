"""Verify the current embedded pack against the running isolated test server."""
from pathlib import Path
import hashlib, urllib.request, urllib.error, zipfile

root = Path(__file__).resolve().parents[1]
with zipfile.ZipFile(root / 'EndUpdate-1.0.5.jar') as jar:
    expected = jar.read('endupdate-pack.zip')
digest = hashlib.sha1(expected).hexdigest()
url = f'http://127.0.0.1:25586/endupdate/{digest}.zip'
with urllib.request.urlopen(url, timeout=10) as response:
    assert response.status == 200
    assert response.read() == expected
with urllib.request.urlopen(urllib.request.Request(url, method='HEAD'), timeout=10) as response:
    assert response.status == 200
    assert int(response.headers['Content-Length']) == len(expected)
for address, method, status in [(url + '/missing', 'GET', 404), (url, 'POST', 405)]:
    try:
        urllib.request.urlopen(urllib.request.Request(address, method=method), timeout=10)
        raise AssertionError(f'Expected {status}')
    except urllib.error.HTTPError as error:
        assert error.code == status
report = f'GET 200: {len(expected)} bytes\nSHA1 verified: {digest}\nHEAD 200; unknown path 404; POST 405\n'
(root / 'verification/http-pack.txt').write_text(report, 'utf-8')
print(report)

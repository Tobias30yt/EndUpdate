"""Compatibility entry point: import the current vanilla-style texture set."""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).with_name('prepare_vanilla_assets.py')), run_name='__main__')

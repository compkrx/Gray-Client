#!/usr/bin/env python3
"""Run Gradle with the cloud's existing proxy, without copying credentials."""
import os
import subprocess
import sys
from pathlib import Path
from urllib.parse import urlsplit
args = []
for protocol in ('http', 'https'):
    proxy = os.environ.get(protocol.upper() + '_PROXY')
    if not proxy:
        continue
    parsed = urlsplit(proxy)
    if parsed.username or parsed.password:
        raise SystemExit('Use the platform proxy without credentials in the URL.')
    if parsed.hostname:
        args.extend([f'-D{protocol}.proxyHost={parsed.hostname}',
                     f'-D{protocol}.proxyPort={parsed.port or 80}'])
args.extend(['-Dhttp.nonProxyHosts=localhost|127.*|[::1]', '-Dhttps.nonProxyHosts=localhost|127.*|[::1]'])
# The cloud's managed CA store includes its HTTPS proxy's signing certificate.
# Keep chain verification enabled when using a separately downloaded JDK.
trust_store = Path('/etc/ssl/certs/java/cacerts')
if trust_store.is_file():
    args.append(f'-Djavax.net.ssl.trustStore={trust_store}')
env = os.environ.copy()
env['GRADLE_USER_HOME'] = '/workspace/.gradle'
raise SystemExit(subprocess.call([sys.argv[1], *args, *sys.argv[2:]], env=env))

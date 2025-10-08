# Local Development Setup
## On Ubuntu VM
### 1. Edit Docker daemon configuration
```shell
sudo mkdir -p /etc/systemd/system/docker.service.d
sudo nano /etc/systemd/system/docker.service.d/override.conf
```
### 2. Add this content
```shell
[Service]
ExecStart=
ExecStart=/usr/bin/dockerd -H fd:// -H tcp://0.0.0.0:2375
```
### 3. Restart Docker
```shell
sudo systemctl daemon-reload
sudo systemctl restart docker
```
### 4. Verify it's listening
```shell
sudo netstat -tulpn | grep 2375
```
### 5. Get VM IP address
```shell
ip addr show
```
Example: `inet 192.168.165.141/24`. It will be used in the config below.

## On Host Machine (Windows/Mac)
Set environment variable before running tests
### Windows (PowerShell)
```shell
$env:DOCKER_HOST="tcp://192.168.165.141"
mvn test
```
### Windows (CMD)
```shell
DOCKER_HOST=tcp://192.168.165.141
mvn test
```
### Mac/Linux
```shell
export DOCKER_HOST=tcp://192.168.165.141
mvn test
```
### Set in IntelliJ IDEA:
- Run → Edit Configurations
- Environment variables: `DOCKER_HOST=tcp://192.168.165.141`

### Create a Testcontainers config
On your host machine, create this file:
```
/Users/<your-username>/.testcontainers.properties   # macOS
~/.testcontainers.properties                        # Linux
C:\Users\<your-username>\.testcontainers.properties  # Windows
```
And add the following content (replace the IP with your VM’s):
```properties
docker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy
docker.host=tcp://192.168.165.141:2375
testcontainers.host.override=192.168.165.141
```
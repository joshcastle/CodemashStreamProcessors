Add the helm Spring Boot Library:
TBD

Install with helm:
* `helm upgrade --install ghost-occupation library/spring-boot -f cd/values.yaml`

Uninstall with helm: 
* `helm delete ghost-occupation`

Endpoint:
* `8080/ghost/occupation/{ghost}`
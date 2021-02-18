# Num-portal Pipelines

## Description

### Test Stage

During test stage the application runs the unit tests, integrations tests and dependency-check and stores those as artifacts in CircleCI.
In Artifacts tab in CircleCi you can find the code coverage report of JaCoCo and the dependency-check.html to visualize the 
results of the dependency analysis.

The final step is the sonarscan, which simply uses the CircleCi sonarcloud orb to send the service to analysis to Sonarcloud.


### Version Tag Stage

Is done for branches that follow the following regex naming conventions `^(release\/v\d+\.\d+\.\d+|hotfix\/v\d+\.\d+\.\d+)$`

For example following branches would execute this step `release/v1.0.1` and `hotfix/v1.0.1`. With these examples
Maven package version would be updated with the given version, and a new commit with tag v1.0.1 would be created and pushed to git.

### Build and Deploy Stage

Is done for branches that follow the following regex naming conventions `^(release\/v\d+\.\d+\.\d+|hotfix\/v\d+\.\d+\.\d+|master|develop)$`.

Meaning the branches executed in the previous step will also execute here, in addition develop and master branches
will execute here.

In the case of release and hotfix branches a new Docker image is built that has a tag with the version plus suffix -rc, 
for example v1.0.1-rc. In the case of master branch the version is pulled from the POM.xml, and that is used as a tag
and in the case of develop, an image with a tag `develop` is pushed.


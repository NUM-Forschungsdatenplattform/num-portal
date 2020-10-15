# Num-portal Pipelines

## Description

### Test Stage

TODO not implemented yet, but would contain jobs for testing, doing CVE scan and running the Solar for the branch.

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

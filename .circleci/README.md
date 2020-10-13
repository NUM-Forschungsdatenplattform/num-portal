# Num-portal Pipelines

## Description

Currently, the pipeline has been divided into 3 different stages, which are executed
in sequential order. First phase is testing where we would run the different quality 
steps, like linting, testing, sonar and CVE-scan. These are not currently implemented
and could also be divided into separate jobs that could be run in parallel.

Second step is only executed in master branch, and it uses the `npm version` to
bump the application version in package.json and to push that version bump alongside
with appropriate tags into master branch. In the .npmrc we have defined that the version
commit from npm always includes message `[skip ci]`, which skips the ci for that commit.

In the third step the Docker image is built, given a tag and sent to docker registry.
If the branch is a feature branch, this step isn't executed and if the branch
is staging or develop, the branch name is used as a tag, and in master the latest
version found from the package.json is used as the tag. The Docker registry credentials
are stored and fetch from the CircleCI.

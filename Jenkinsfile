#!/usr/bin/env groovy

/*dockerfile {
    slackChannel = ''
    upstreamProjects = 'confluentinc/schema-registry'
    extraDeployArgs = '-Ddocker.skip=true'
    dockerPush = false
    dockerScan = false
    dockerImageClean = false
    testbreakReporting = false
}*/

//TODO: Does ksql project need to use dockerfile.groovy if it doesn't build docker images?

common {
  slackChannel = ''
  upstreamProjects = 'confluentinc/schema-registry'
  extraDeployArgs = '-Ddocker.skip=true'
  testbreakReporting = false
  timeoutHours = 3
  downStreamRepos = ["confluent-security-plugins"]
  disableConcurrentBuilds = true
}

pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        sh '''chmod +x gradlew
./gradlew clean build'''
        archiveArtifacts(artifacts: 'build/libs/*.jar', fingerprint: true)
      }
    }

  }
}
def getVersion() {
    return sh(returnStdout: true, script: 'git describe --abbrev=0 --tags').trim()
}

def isTag() {
    String tag = sh(returnStdout: true, script: 'git tag --points-at HEAD').trim()
    return tag != ''
}

def getVersion() {
    echo "1.2.42"
    // return sh(returnStdout: true, script: 'git describe --abbrev=0 --tags').trim()
}

def isTag() {
    // String lastCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
    // String tag = sh(returnStdout: true, script: "git show-ref --tags -d | grep ^${lastCommit} | sed -e 's,.* refs/tags/,,' -e 's/\\^{}//'").trim()
    // return tag != ''
    return false
}
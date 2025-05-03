/*
 * Copyright 2013-2025, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nextflow.lineage

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

/**
 * Lineage History file tests
 *
 * @author Jorge Ejarque <jorge.ejarque@seqera.io>
 */
class DefaultLinHistoryLogTest extends Specification {

    Path tempDir
    Path historyFile
    DefaultLinHistoryLog linHistoryLog

    def setup() {
        tempDir = Files.createTempDirectory("wdir")
        historyFile = tempDir.resolve("lin-history")
        linHistoryLog = new DefaultLinHistoryLog(historyFile)
    }

    def cleanup(){
        tempDir?.deleteDir()
    }

    def "write should add a new file to the history folder"() {
        given:
        UUID sessionId = UUID.randomUUID()
        def runName = "TestRun"
        def launchLid = "lid://123"
        def runLid = "lid://456"

        when:
        linHistoryLog.write(runName, sessionId, launchLid, runLid)

        then:
        def files = historyFile.listFiles()
        files.size() == 1
        def parsedRecord = LinHistoryRecord.parse(files[0].text)
        parsedRecord.sessionId == sessionId
        parsedRecord.runName == runName
        parsedRecord.launchLid == launchLid
        parsedRecord.runLid == runLid
    }

    def "should return correct record for existing session"() {
        given:
        UUID sessionId = UUID.randomUUID()
        def runName = "Run1"
        def launchLid = "lid://123"
        def runLid = "lid://456"

        and:
        linHistoryLog.write(runName, sessionId, launchLid, runLid)

        when:
        def record = linHistoryLog.getRecord(sessionId)
        then:
        record.sessionId == sessionId
        record.runName == runName
        record.launchLid == launchLid
        record.runLid == runLid
    }

    def "should return null and warn if session does not exist"() {
        expect:
        linHistoryLog.getRecord(UUID.randomUUID()) == null
    }

    def "update should modify existing Lid for given session"() {
        given:
        UUID sessionId = UUID.randomUUID()
        def runName = "Run1"
        def launchLid = "launch-lid"
        def runLid = "run-lid"

        and:
        linHistoryLog.write(runName, sessionId, launchLid, '-')

        when:
        linHistoryLog.updateRunLid(sessionId, runLid)

        then:
        def files = historyFile.listFiles()
        files.size() == 1
        def parsedRecord = LinHistoryRecord.parse(files[0].text)
        parsedRecord.runLid == runLid
    }

    def "update should do nothing if session does not exist"() {
        given:
        UUID existingSessionId = UUID.randomUUID()
        UUID nonExistingSessionId = UUID.randomUUID()
        def runName = "Run1"
        def launchLid = "lid://123"
        def runLid = "lid://456"
        and:
        linHistoryLog.write(runName, existingSessionId, launchLid, '-')

        when:
        linHistoryLog.updateRunLid(nonExistingSessionId, runLid)
        then:
        def files = historyFile.listFiles()
        files.size() == 1
        def parsedRecord = LinHistoryRecord.parse(files[0].text)
        parsedRecord.runLid == '-'
    }

    def 'should get records' () {
        given:
        UUID sessionId = UUID.randomUUID()
        def runName = "Run1"
        def launchLid = "lid://123"
        def runLid = "-"
        and:
        linHistoryLog.write(runName, sessionId, launchLid, runLid)

        when:
        def records = linHistoryLog.getRecords()
        then:
        records.size() == 1
        records[0].sessionId == sessionId
        records[0].runName == runName
        records[0].launchLid == launchLid
        records[0].runLid == runLid
    }
}


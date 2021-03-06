package com.mongodb.stitch.android.services.mongodb.performance

import com.google.android.gms.tasks.Tasks
import org.bson.BsonDouble
import org.bson.BsonValue
import org.bson.Document
import org.bson.types.ObjectId

class SyncR2LOnlyPerformanceTestDefinitions {
    companion object {

        /*
         * Before: Perform remote insert of numDoc documents
         * Test: Configure sync to sync on the inserted docs and perform a sync pass
         * After: Ensure that the initial sync worked as expected
         */
        fun testInitialSync(testHarness: SyncPerformanceIntTestsHarness, runId: ObjectId) {
            val testName = "R2L_InitialSync"

            // Local variable for list of documents captured by the test definition closures below.
            // This should change for each iteration of the test.
            var documentIdsForCurrentTest = mutableListOf<BsonValue>()

            // Initial sync for a purely R2L Scenario means inserting remote documents and then
            // configuring syncMany() on the inserted document id's and performing a sync pass.
            testHarness.runPerformanceTestWithParams(
                testName, runId,
                beforeEach = { ctx, numDocs, docSize ->
                    // Generate the documents that are to be synced via R2L and remotely insert them
                    documentIdsForCurrentTest.clear()
                    documentIdsForCurrentTest.addAll(SyncPerformanceTestUtils.insertToRemote(
                        ctx, numDocs, docSize
                    ))
                },
                testDefinition = { ctx, _, _ ->
                    val sync = ctx.testColl.sync()

                    // If sync fails for any reason, halt the test
                    SyncPerformanceTestUtils.defaultConfigure(ctx)

                    // Sync() on all of the inserted document ids
                    Tasks.await(sync.syncMany(*(documentIdsForCurrentTest!!.toTypedArray())))

                    // Perform syncPass() and halt the test if the pass fails
                    SyncPerformanceTestUtils.doSyncPass(ctx)
                },
                afterEach = { ctx, numDocs, _ ->
                    // Verify that the test did indeed synchronize the provided documents locally
                    SyncPerformanceTestUtils.assertLocalAndRemoteDBCount(ctx, numDocs)
                }
            )
        }

        /*
         * Before: Perform remote insert of numDoc documents
         *         Configure sync(), perform sync pass, disconnect networkMonitor
         * Test: Reconnect the network monitor and perform sync pass
         * After: Ensure that the sync pass worked as expected
         */
        fun testDisconnectReconnect(testHarness: SyncPerformanceIntTestsHarness, runId: ObjectId) {
            val testName = "R2L_DisconnectReconnect"

            testHarness.runPerformanceTestWithParams(
                testName, runId,
                beforeEach = { ctx, numDocs, docSize ->
                    val sync = ctx.testColl.sync()

                    // Generate the documents that are to be synced via R2L and insert remotely
                    val ids = SyncPerformanceTestUtils.insertToRemote(
                        ctx, numDocs, docSize
                    )

                    // If sync fails for any reason, halt the test
                    SyncPerformanceTestUtils.defaultConfigure(ctx)

                    // Sync() on all of the inserted document ids
                    Tasks.await(sync.syncMany(*(ids.toTypedArray())))

                    // Perform sync pass, it will throw an exception if it fails
                    SyncPerformanceTestUtils.doSyncPass(ctx)

                    // Verify that the test did indeed synchronize the provided documents locally
                    val numSyncedIds = Tasks.await(sync.syncedIds).size
                    val numLocalDocs = Tasks.await(sync.count())

                    SyncPerformanceTestUtils.assertIntsAreEqualOrThrow(
                        numSyncedIds, numDocs, "Number of Synced Ids")
                    SyncPerformanceTestUtils.assertIntsAreEqualOrThrow(
                        numLocalDocs.toInt(), numDocs, "Number of Local Documents")

                    // Disconnect the DataSynchronizer and wait
                    // for the underlying streams to close
                    ctx.testNetworkMonitor.connectedState = false
                    while (ctx.testDataSynchronizer.areAllStreamsOpen()) {
                        testHarness.logMessage("waiting for streams to close")
                        Thread.sleep(1000)
                    }
                },
                testDefinition = { ctx, _, _ ->
                    // Reconnect the DataSynchronizer, and wait for the streams to reopen. The
                    // stream being open indicates that the doc configs are now set as stale.
                    // Check every 10ms so we're not doing too much work on this thread, and
                    // don't log anything, so as not to pollute the test results with logging
                    // overhead.
                    ctx.testNetworkMonitor.connectedState = true
                    var counter = 0
                    while (!ctx.testDataSynchronizer.areAllStreamsOpen()) {
                        Thread.sleep(10)

                        // if this hangs longer than 30 seconds, throw an error
                        counter += 1
                        if (counter > 3000) {
                            testHarness.logMessage("stream never opened after reconnect")
                            error("stream never opened after reconnect")
                        }
                    }

                    // Do the sync pass that will perform the stale document fetch
                    val syncPassSucceeded = ctx.testDataSynchronizer.doSyncPass()

                    // Perform syncPass() and halt the test if the pass fails
                    if (!syncPassSucceeded) {
                        error("sync pass failed")
                    }
                },
                afterEach = { ctx, numDocs, _ ->
                    // Verify that the test did indeed synchronize the updates locally
                    SyncPerformanceTestUtils.assertLocalAndRemoteDBCount(ctx, numDocs)
                }
            )
        }

        /*
         * Before: Perform remote insert of numDoc documents, configure sync(), perform sync pass
         *         Update numChangeEvent documents remotely, and numConflict documents locally
         * Test: Perform sync pass
         * After: Ensure that the sync pass worked properly
         */
        fun testSyncPass(testHarness: SyncPerformanceIntTestsHarness, runId: ObjectId) {
            // Run doTestSyncPass() for all changeEvent Percentages found in SyncPerfTestUtils
            SyncPerformanceTestUtils.getChangeEventPercentages().forEach { changeEventPercentage ->
                SyncPerformanceTestUtils.getConflictPercentages().forEach { conflictPercentage ->
                    doTestSyncPass(testHarness, runId, changeEventPercentage, conflictPercentage)
                }
            }
        }

        private fun doTestSyncPass(
            testHarness: SyncPerformanceIntTestsHarness,
            runId: ObjectId,
            pctOfDocsWithChangeEvents: Double,
            pctOfDocsWithConflicts: Double
        ) {
            val testName = "R2L_SyncPass"

            // Local variable for the number of docs updated in the test
            // This should change for each iteration of the test.
            var numberOfChangedDocs: Int = -1

            testHarness.runPerformanceTestWithParams(
                testName, runId,
                beforeEach = { ctx, numDocs: Int, docSize: Int ->
                    val sync = ctx.testColl.sync()

                    // Generate the documents that are to be synced via R2L and insert remotely
                    var ids = SyncPerformanceTestUtils.insertToRemote(
                        ctx, numDocs, docSize
                    )

                    // If sync fails for any reason, halt the test
                    SyncPerformanceTestUtils.defaultConfigure(ctx)

                    // Sync on the ids inserted remotely
                    Tasks.await(sync.syncMany(*(ids.toTypedArray())))

                    // Perform sync pass, it will throw an exception if it fails
                    SyncPerformanceTestUtils.doSyncPass(ctx)

                    // Verify that the test did indeed synchronize the provided documents locally
                    SyncPerformanceTestUtils.assertLocalAndRemoteDBCount(ctx, numDocs)

                    // Shuffle the documents
                    ids = ids.shuffled()

                    // Remotely update the desired percentage of documents and check it works
                    var numChange = (pctOfDocsWithChangeEvents * numDocs).toInt()
                    SyncPerformanceTestUtils.performRemoteUpdate(ctx, ids.subList(0, numChange))
                    numberOfChangedDocs = numChange

                    // Locally update the desired percentage of documents and check it works
                    numChange = (pctOfDocsWithChangeEvents * pctOfDocsWithConflicts * numDocs).toInt()
                    SyncPerformanceTestUtils.performLocalUpdate(ctx, ids.subList(0, numChange))
                },
                testDefinition = { ctx, _, _ ->
                    // Do the sync pass that will sync the remote changes to the local collection
                    // Perform syncPass() and halt the test if the pass fails
                    SyncPerformanceTestUtils.doSyncPass(ctx)
                },
                afterEach = { ctx, numDocs: Int, _ ->
                    // Verify that the test did indeed synchronize the updates locally
                    SyncPerformanceTestUtils.assertLocalAndRemoteDBCount(ctx, numDocs)

                    // Verify the updates were applied locally
                    val numRemoteUpdates = numberOfChangedDocs ?: -1

                    // Both the local and remote should have
                    //      numRemoteUpdates documents with {newField: "remote"}
                    var res = Tasks.await(ctx.testColl.count(Document("newField", "remote")))
                    SyncPerformanceTestUtils.assertIntsAreEqualOrThrow(
                        res.toInt(), numRemoteUpdates, "Num Remote Updates After Test"
                    )

                    res = Tasks.await(ctx.testColl.sync().count(Document("newField", "remote")))
                    SyncPerformanceTestUtils.assertIntsAreEqualOrThrow(
                        res.toInt(), numRemoteUpdates, "Num Local Updates After Test"
                    )
                }, extraFields = mapOf(
                    "percentageChangeEvent" to BsonDouble(pctOfDocsWithChangeEvents),
                    "percentageConflict" to BsonDouble(pctOfDocsWithConflicts)
                )
            )
        }
    }
}

/*
 * Under LGPL
 * by George Giannakopoulos
 */

package gr.demokritos.iit.jinsect.structs;

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author pckid
 */
public class ArrayGraph {
    protected int gLength;
    // the document's graph
    protected DocumentNGramGraph gRes;

    public void SetLengthLocator(int length){
        gLength = length;
    }

    public DocumentNGramGraph getGraphForArray(int[][] ImageArray,
            int iWindowSize, int noisy_graph_threshold)
    {
        final HashMap hNull = null;

        final int iWidthArg = ImageArray.length;
        final int iHeightArg = ImageArray[0].length;
        final int iWindowSizeArg = iWindowSize;

        EdgeCachedLocator eclLocator = new EdgeCachedLocator(gLength * gLength);
        // Init graph
        gRes = new DocumentNGramSymWinGraph(1,1,1);
        gRes.setDataString("");
        gRes.setLocator(eclLocator);

        // queue for threads in order to be executed
        ExecutorService tqThreads = Executors.newCachedThreadPool();
        
        // OBSOLETE
        //ThreadQueue tqThreads = new ThreadQueue();

        final DocumentNGramGraph gResArg = gRes;
        final int[][] ImageSegmentedArray = ImageArray;
        // For every point
        for(int iXCnt = 0; iXCnt < iWidthArg; iXCnt++) {
            // Multi-thread arguments (instant for arguments)
            // constant copies of useful arguments
            final int iXCntArg = iXCnt;

            // Runnable is an interface only to run
            // that's why (interface)
//            while (!
            tqThreads.submit(new Runnable() {
                @Override
                public void run() {
                    for (int iYCnt = 0; iYCnt < iHeightArg; iYCnt++) {
                        //DEBUG LINES
//                        synchronized (System.err) {
//                            synchronized (gResArg) {
//                                System.err.println("Before:\n" + iXCntArg + "," +
//                                        iYCnt + "\n" +
//                                        gr.demokritos.iit.jinsect.utils.graphToDot(
//                                    gResArg.getGraphLevel(0), false));
//                            }
//                        }
                        /////////////
                        // Create label for current pixel
                        String sCur = Integer.toString(ImageSegmentedArray[iXCntArg][iYCnt]);
                        for (int iWinX = -(int)(iWindowSizeArg / 2);
                          iWinX <= (int)(iWindowSizeArg / 2); iWinX++) {
                            for (int iWinY = -(int)(iWindowSizeArg / 2);
                              iWinY <= (int)(iWindowSizeArg / 2); iWinY++) {
                                ArrayList<String> lNeighbours = new ArrayList<String>();
                                // Ignore self
                                if ((iWinX == 0) && (iWinY == 0))
                                    continue;
                                // If within limits
                                if (between(iXCntArg + iWinX, 0, iWidthArg-1)
                                    && between(iYCnt + iWinY, 0, iHeightArg-1)) {

                                    lNeighbours.add(Integer.toString(
                                            ImageSegmentedArray[iXCntArg + iWinX][iYCnt + iWinY]));

                                    synchronized (gResArg) {
                                        gResArg.createEdgesConnecting(
                                            gResArg.getGraphLevel(0),
                                            sCur, lNeighbours, null);
                                    }
                                    ///////////////////////////
                                }
                            }
                        }
                        //DEBUG LINES
//                        synchronized (System.err) {
//                            synchronized (gResArg) {
//                                System.err.println("After:\n" + iXCntArg + "," + iYCnt + "\n" +
//                                        gr.demokritos.iit.jinsect.utils.graphToDot(
//                                    gResArg.getGraphLevel(0), false));
//                            }
//                        }
                        /////////////
                    }
                }
            });
//                    )
//                // be patient (for the basic thread)
//                Thread.yield();

        }

        try {
            // maybe there are threads that have not finished, so wait for them
            tqThreads.shutdown();
            tqThreads.awaitTermination(1000, TimeUnit.DAYS);
        }
        catch (InterruptedException ie) {
            // Ignore
            System.err.println("Interrupted creation.");
            return null;
        }
        return gRes;
    }

    /**
     * This function returns true when the iNum is between the limits [iMin,iMax]
     * @param iNum the number of interest
     * @param iMin the lowest bound of the range
     * @param iMax the highest bound of the range
     * @return true if iNum is inside the range or false if not
     */
    public final boolean between(int iNum, int iMin, int iMax) {
        return (iNum >= iMin) && (iNum <= iMax);
    }

    /** Testing function. */
    public static void main(String[] sArgs) {
        int[][] iaTest = new int[1][2];
        for (int iX=0; iX<iaTest.length; iX++) {
            for (int iY=0; iY < iaTest[0].length; iY++) {
                iaTest[iX][iY] = iX;
            System.out.print(iX);
            }
            System.out.println();
        }
        ArrayGraph atg = new ArrayGraph();
        System.err.println(gr.demokritos.iit.jinsect.utils.graphToDot(
                atg.getGraphForArray(iaTest, 2, 100000).getGraphLevel(0), true));
    }
}

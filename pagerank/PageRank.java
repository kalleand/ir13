package pagerank;

/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 */  

import java.util.*;
import java.io.*;


public class PageRank{

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 10000;

    /**
     *   Mapping from document names to document numbers.
     */
    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a Hashtable, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a Hashtable whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    Hashtable<Integer,Hashtable<Integer,Boolean>> link = new Hashtable<Integer,Hashtable<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The number of documents with no outlinks.
     */
    int numberOfSinks = 0;

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     * Probability of staying and continue to jump.
     */
    final static double C = (1-BORED);

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    /**
     *   Never do more than this number of iterations regardless
     *   of whether the transistion probabilities converge or not.
     */
    final static int MAX_NUMBER_OF_ITERATIONS = 1000;

    /**
     * G matrix
     */
    double[][] gMatrix = new double[MAX_NUMBER_OF_DOCS][MAX_NUMBER_OF_DOCS];


    /* --------------------------------------------- */


    public PageRank( String filename ) {
        int noOfDocs = readDocs( filename );
        computePagerank( noOfDocs );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and creates the docs table. When this method 
     *   finishes executing then the @code{out} vector of outlinks is 
     *   initialised for each doc, and the @code{p} matrix is filled with
     *   zeroes (that indicate direct links) and NO_LINK (if there is no
     *   direct link. <p>
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
        int fileIndex = 0;
        try {
            System.err.print( "Reading file... " );
            BufferedReader in = new BufferedReader( new FileReader( filename ));
            String line;
            while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
                int index = line.indexOf( ";" );
                String title = line.substring( 0, index );
                Integer fromdoc = docNumber.get( title );
                //  Have we seen this document before?
                if ( fromdoc == null ) {	
                    // This is a previously unseen doc, so add it to the table.
                    fromdoc = fileIndex++;
                    docNumber.put( title, fromdoc );
                    docName[fromdoc] = title;
                }
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
                while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
                    String otherTitle = tok.nextToken();
                    Integer otherDoc = docNumber.get( otherTitle );
                    if ( otherDoc == null ) {
                        // This is a previousy unseen doc, so add it to the table.
                        otherDoc = fileIndex++;
                        docNumber.put( otherTitle, otherDoc );
                        docName[otherDoc] = otherTitle;
                    }
                    // Set the probability to 0 for now, to indicate that there is
                    // a link from fromdoc to otherDoc.
                    if ( link.get(fromdoc) == null ) {
                        link.put(fromdoc, new Hashtable<Integer,Boolean>());
                    }
                    if ( link.get(fromdoc).get(otherDoc) == null ) {
                        link.get(fromdoc).put( otherDoc, true );
                        out[fromdoc]++;
                    }
                }
            }
            if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
                System.err.print( "stopped reading since documents table is full. " );
            }
            else {
                System.err.print( "done. " );
            }
            // Compute the number of sinks.
            for ( int i=0; i<fileIndex; i++ ) {
                if ( out[i] == 0 )
                    numberOfSinks++;
            }
        }
        catch ( FileNotFoundException e ) {
            System.err.println( "File " + filename + " not found!" );
        }
        catch ( IOException e ) {
            System.err.println( "Error reading file " + filename );
        }
        System.err.println( "Read " + fileIndex + " number of documents" );
        return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Computes the pagerank of each document.
     */
    void computePagerank( int numberOfDocs ) {
        // double[] x = powerIteration(numberOfDocs); /* 2.3 */
        // double[] x = approxPageRank(numberOfDocs); /* 2.4 part 1 */
        double[] x = monteCarlo(numberOfDocs); /* 2.4 part 2 */



        // Store our result in an ArrayList (any class implementing
        // Collections would do) to sort it using library sorting
        // function. To be able to do this we use a inner class 
        // which implements Comparable.
        ArrayList<CompareObj> al = new ArrayList<CompareObj>();
        for(int i = 0; i < numberOfDocs; i++)
        {
            al.add(new CompareObj(i, x[i]));
        }
        Collections.sort(al);

        // Prints the 50 top scores.
        for(int i = 0; i < 50; i++)
        {
            // We want the docName instead of the docNumber because docNumber can differ
            // between executions of the pagerank whereas docName stays the same.
            // (Both are Integers of links1000.txt and links10000.txt)
            System.out.println((i+1) + ".\t" + docName[al.get(i).key] + "\t" + al.get(i).val);
        }
    }

    private void generateG(int numberOfDocs)
    {
        double jMatrix = 1 / numberOfDocs;
        // Creating P
        double[][] pMatrix = new double[numberOfDocs][numberOfDocs];
        for(int i = 0; i < numberOfDocs; i++)
        {
            int numberOfLinks = out[i];
            if(numberOfLinks == 0)
            {
                double valueOfLink = 1 / (double) (numberOfDocs - 1);
                for(int j = 0; j < numberOfDocs; j++)
                {
                    if(j == i) 
                        continue;
                    else
                        pMatrix[i][j] = valueOfLink;
                }
            }
            else
            {
                double valueOfLink = 1 / (double) numberOfLinks;
                Hashtable<Integer, Boolean> outLinks = link.get(i);
                for(Integer key : outLinks.keySet())
                {
                    if(outLinks.get(key) == true)
                    {
                        pMatrix[i][key] = valueOfLink;
                    }
                }
            }
        }

        // Creating G. 
        for(int i = 0; i < numberOfDocs; i++)
        {
            for(int j = 0; j < numberOfDocs; j++)
            {
                gMatrix[i][j] = C * pMatrix[i][j] + (1 - C) * jMatrix;
            }
        }
    }

    /**
     * MC v 4
     */
    private double[] monteCarlo(int numberOfDocs)
    {
        int mMax = 100;
        int maxLength = 5;

        int totalNumberOfVisits = 0;

        Random rand = new Random();

        int[] numberOfVisits = new int[numberOfDocs];


        for(int m = 0; m < mMax; m++)
        {
            for(int n = 0; n < numberOfDocs; n++)
            {
                int currentPage = n;
                for(int i = 0; i < maxLength; i++)
                {
                    numberOfVisits[currentPage]++;
                    totalNumberOfVisits++;
                    if(out[currentPage] == 0)
                    {
                        // This is a sink.
                        break;
                    }
                    else
                    {
                        // Gå till nästa.
                        // Antingen hoppa till nästa länk eller till en random sida.
                        if(rand.nextDouble() < C)
                        {
                            Hashtable<Integer, Boolean> table = link.get(currentPage);
                            int next = rand.nextInt(table.size());
                            int a = 0;
                            for(Integer j : table.keySet())
                            {
                                if(a == next)
                                {
                                    currentPage = j;
                                    break;
                                }
                                a++;
                            }
                        }
                        else
                        {
                            int next = rand.nextInt(numberOfDocs);
                            while(next == currentPage)
                            {
                                next = rand.nextInt(numberOfDocs);
                            }
                            currentPage = next;
                        }
                    }
                }
            }
        }
        double[] ret = new double[numberOfDocs];
        for(int i = 0; i < numberOfDocs; i++)
        {
            ret[i] = (double) numberOfVisits[i] / (double) totalNumberOfVisits;
        }
        return ret;
    }

    private double[] approxPageRank(int numberOfDocs)
    {
        generateG(numberOfDocs);
        double[] x = new double[numberOfDocs];
        double[] xPrim = new double[numberOfDocs];

        // Initial guess.
        x[0] = 1;
        for(int iters = 0; iters < 1000; iters++)
        {
            for(int i = 0; i < numberOfDocs; i++)
            {
                // Get the lists i -> j
                if(out[i] > 0){
                    Hashtable<Integer, Boolean> outlinks = link.get(i);
                    for( Integer j : outlinks.keySet() )
                    {
                        xPrim[j] += x[i] * C / out[i];
                    }
                }
                xPrim[i] += (1 - C) / numberOfDocs;
                xPrim[i] += numberOfSinks / (numberOfDocs * numberOfDocs);
            }

            for(int i = 0; i < numberOfDocs; i++)
            {
                x[i] = xPrim[i];
                xPrim[i] = 0;
            }
        }
        return x;
    }

    private double[] powerIteration(int numberOfDocs)
    {
        generateG(numberOfDocs);
        double[] x = new double[numberOfDocs];
        double[] xPrim = new double[numberOfDocs];

        // Initial guess.
        double initValue = 1 / (double) numberOfDocs;
        for(int i = 0; i < numberOfDocs; i++)
        {
            xPrim[i] = initValue;
        }

        // Loop until we found a stable distribution.
        int numberOfIter = 0;
        while(calculateDiff(x,xPrim, numberOfDocs) > EPSILON)
        {
            numberOfIter++;
            for(int i = 0; i < numberOfDocs; i++)
            {
                x[i] = xPrim[i];
            }

            for(int i = 0; i < numberOfDocs; i++)
            {
                double newVal = 0;
                for(int j = 0; j < numberOfDocs; j++)
                {
                    newVal += x[j] * gMatrix[j][i];
                }
                xPrim[i] = newVal;
            }

        }
        double total = 0;
        for(int i = 0; i < numberOfDocs; i++)
        {
            total+=xPrim[i];
        }
        for(int i = 0; i < numberOfDocs; i++)
        {
            xPrim[i] = xPrim[i] / total;
        }
        return xPrim;
    }

    private double calculateDiff(double[] a, double[] b, int numberOfDocs)
    {
        double res = 0;
        for(int i = 0; i < numberOfDocs; i++)
        {
            res += Math.abs(a[i] - b[i]);
        }
        return res;
    }

    private class CompareObj implements Comparable
    {
        public int key;
        public double val;

        public CompareObj(int key, double val)
        {
            this.key = key;
            this.val = val;
        }

        public int compareTo(Object obj)
        {
            if( obj instanceof CompareObj )
            {
                return (int) Double.compare(((CompareObj) obj).val, this.val);
            }
            else
            {
                return -1;
            }
        }
    }

    /* --------------------------------------------- */


    public static void main( String[] args ) {
        if ( args.length != 1 ) {
            System.err.println( "Please give the name of the link file" );
        }
        else {
            new PageRank( args[0] );
        }
    }
}

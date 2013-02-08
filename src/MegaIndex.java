/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012
 */  

package ir;

import com.larvalabs.megamap.MegaMapManager;
import com.larvalabs.megamap.MegaMap;
import com.larvalabs.megamap.MegaMapException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;


public class MegaIndex implements Index {

    /** 
     *  The index as a hash map that can also extend to secondary 
     *	memory if necessary. 
     */
    private MegaMap index;


    /** 
     *  The MegaMapManager is the user's entry point for creating and
     *  saving MegaMaps on disk.
     */
    private MegaMapManager manager;


    /** The directory where to place index files on disk. */
    private static final String path = ".";

    /** Number of documents in the index. */
    private int numberOfDocs = -2;

    /**
     *  Create a new index and invent a name for it.
     */
    public MegaIndex() {
        try {
            manager = MegaMapManager.getMegaMapManager();
            index = manager.createMegaMap( generateFilename(), path, true, false );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }


    /**
     *  Create a MegaIndex, possibly from a list of smaller
     *  indexes.
     */
    public MegaIndex( LinkedList<String> indexfiles ) {
        try {
            manager = MegaMapManager.getMegaMapManager();
            if ( indexfiles.size() == 0 ) {
                // No index file names specified. Construct a new index and
                // invent a name for it.
                index = manager.createMegaMap( generateFilename(), path, true, false );
                
            }
            else if ( indexfiles.size() == 1 ) {
                // Read the specified index from file
                index = manager.createMegaMap( indexfiles.get(0), path, true, false );
                HashMap<String,String> m = (HashMap<String,String>)index.get( "..docIDs" );
                if ( m == null ) {
                    System.err.println( "Couldn't retrieve the associations between docIDs and document names" );
                }
                else {
                    docIDs.putAll( m );
                }
                HashMap<String,Integer> n = (HashMap<String, Integer>)index.get( "..docLengths" );
                if( n == null )
                {
                    System.err.println( "Could not retrieve the length of the documents." );
                }
                else
                {
                    docLengths.putAll(n);
                }
            }
            else {
                // Merge the specified index files into a large index.
                MegaMap[] indexesToBeMerged = new MegaMap[indexfiles.size()];
                for ( int k=0; k<indexfiles.size(); k++ ) {
                    System.err.println( indexfiles.get(k) );
                    indexesToBeMerged[k] = manager.createMegaMap( indexfiles.get(k), path, true, false );
                }
                index = merge( indexesToBeMerged );
                for ( int k=0; k<indexfiles.size(); k++ ) {
                    manager.removeMegaMap( indexfiles.get(k) );
                }
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }


    /**
     *  Generates unique names for index files
     */
    String generateFilename() {
        String s = "index_" + Math.abs((new java.util.Date()).hashCode());
        System.err.println( s );
        return s;
    }


    /**
     *   It is ABSOLUTELY ESSENTIAL to run this method before terminating 
     *   the JVM, otherwise the index files might become corrupted.
     */
    public void cleanup() {
        // Save the docID-filename association list in the MegaMap as well
        index.put( "..docIDs", docIDs );
        index.put( "..docLengths", docLengths );
        // Shutdown the MegaMap thread gracefully
        manager.shutdown();
    }



    /**
     *  Returns the dictionary (the set of terms in the index)
     *  as a HashSet.
     */
    public Set getDictionary() {
        return index.getKeys();
    }


    /**
     *  Merges several indexes into one.
     */
    MegaMap merge( MegaMap[] indexes ) {
        try {
            MegaMap res = manager.createMegaMap( generateFilename(), path, true, false );
            for(MegaMap map : indexes) {
                for(String token : (Set<String>)map.getKeys()) {
                    if(token.equals("..docIDs")) {
                        HashMap<String,String> m = (HashMap<String,String>)map.get( "..docIDs" );
                        if ( m == null ) {
                            System.err.println( "Couldn't retrieve the associations between docIDs and document names" );
                        } else {
                            docIDs.putAll( m );
                        }
                        continue;
                    }
                    else if(token.equals("..docLengths")) {
                        
                        HashMap<String,Integer> n = (HashMap<String, Integer>)index.get( "..docLengths" );
                        if( n == null )
                        {
                            System.err.println( "Could not retrieve the length of the documents." );
                        }
                        else
                        {
                            docLengths.putAll(n);
                        }
                    }
                    else if(!res.hasKey(token)) {
                        PostingsList pl = new PostingsList();
                        pl.merge_pl( (PostingsList) map.get(token) );
                        res.put(token, pl);
                    } else {
                        PostingsList pl = (PostingsList) res.get(token);
                        pl.merge_pl( (PostingsList) map.get(token) ); 
                    }
                }
            }
            return res;
        }
        catch ( MegaMapException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        PostingsList current_list = null;
        try {
            current_list = (PostingsList)index.get(token);
        } catch (MegaMapException e) {
            System.out.println("MegaMap BROKE!"); 
        }
        // If the token does not exist yet - create a new postingslist for it
        if(current_list == null) {
            current_list = new PostingsList();
            index.put(token, current_list);
        }

        // Add the docID, offset to the PostingsList
        current_list.add(docID, offset);
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        try {
            return (PostingsList)index.get( token );
        }
        catch( Exception e ) {
            return new PostingsList();
        }
    }


    /**
     *  Searches the index for postings matching the query.
     */
    public PostingsList search( Query query, int queryType, int rankingType ) {
        if(index == null) {
            return null;
        }
        try {
            if(queryType == Index.INTERSECTION_QUERY)
            {
                if( query.size() == 1) {
                    return (PostingsList)index.get(query.terms.getFirst());
                } else {
                    LinkedList<PostingsList> queue = new LinkedList<PostingsList>();
                    for( String str : query.terms ) {
                        PostingsList tmp = (PostingsList)index.get(str);
                        if(tmp == null)
                            return new PostingsList();
                        else
                            queue.add(tmp);
                    }
                    Collections.sort(queue);

                    PostingsList result = queue.pollFirst();
                    while(queue.size() != 0) {
                        result = PostingsList.intersect_query(result, queue.pollFirst());
                    } 
                    return result;
                }
            } else if(queryType == Index.PHRASE_QUERY) {
                if(query.size() == 1) {
                    return (PostingsList)index.get(query.terms.getFirst());
                } else {
                    PostingsList result = (PostingsList)index.get(query.terms.pollFirst());
                    while(query.size() != 0) {
                        PostingsList tmp = (PostingsList)index.get(query.terms.pollFirst());
                        if(tmp == null)
                            return null;
                        result = PostingsList.phrase_query(result, tmp);
                    }
                    return result;
                }
            }
            else if(queryType == Index.RANKED_QUERY)
            {
                PostingsList result = new PostingsList();
                
                if(getPostings(query.terms.getFirst()) == null)
                {
                    return result;
                }

                /*
                try 
                {
                    result = (PostingsList) getPostings(query.terms.getFirst()).clone();
                }
                catch (CloneNotSupportedException e)
                {
                    System.err.println("Could not clone result!");
                }
                */
                for( String term : query.terms )
                {
                    result.union((PostingsList) getPostings(term));
                }
                
                if(numberOfDocs < 0)
                {
                    numberOfDocs = docLengths.keySet().size();
                }


                for( String term : query.terms )
                {
                    PostingsList tmp = (PostingsList) getPostings(term);
                    if(tmp == null) continue;

                    double wtq = Math.log10( numberOfDocs / tmp.size() );
                    for ( PostingsEntry pe : tmp.list )
                    {
                        if(pe.offsets.size() != 0)
                        {
                            result.addScore(pe.docID, (1 + Math.log10(pe.offsets.size())) * wtq);
                            //result.addScore(pe.docID, pe.offsets.size() * wtq);
                        }
                    }
                }
                for ( PostingsEntry pe : result.list )
                {
                    int i = docLengths.get(""+ pe.docID);
                    pe.score /= i;
                }

                Collections.sort(result.list);
                return result;
            }
            else
            {
                return null;
            }
        } catch (MegaMapException e) {
            System.out.println("MegaMap BROKE while searching!");
            e.printStackTrace();
            return null;
        }
    }
}

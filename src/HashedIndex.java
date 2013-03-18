/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012
 */  


package ir;

import java.util.LinkedList;
import java.util.Collections;
import java.util.HashMap;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the index.
     */
    public void insert( String token, int docID, int offset ) {
        PostingsList current_list = index.get(token);

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
        return index.get(token);
    }


    public PostingsList search_wo_sort( Query query, int queryType, int rankingType ) {
        return null;
    }
    /**
     *  Searches the index for postings matching the query.
     */
    public PostingsList search( Query query, int queryType, int rankingType ) {
        // There are 2 types of queries for lab 1.
        if(queryType == Index.INTERSECTION_QUERY)
        {
            if( query.size() == 1) {
                return index.get(query.terms.getFirst());
            } else {
                LinkedList<PostingsList> queue = new LinkedList<PostingsList>();
                for( String str : query.terms ) {
                    PostingsList tmp = index.get(str);
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
                return index.get(query.terms.getFirst());
            } else {
                PostingsList result = index.get(query.terms.pollFirst());
                while(query.size() != 0) {
                    PostingsList tmp = index.get(query.terms.pollFirst());
                    if(tmp == null)
                        return null;
                    result = PostingsList.phrase_query(result, tmp);
                }
                return result;
            }
        }
        return null;
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }

    public int getNumberOfDocs()
    {
        // Not implemented for HashedIndex.
        return 0;
    }
    public void addTerm(int docID, String token)
    {
        // Not implemented for HashedIndex.
    }

    public void setPagerank(HashMap<String, Double> new_pr)
    {
    }
}

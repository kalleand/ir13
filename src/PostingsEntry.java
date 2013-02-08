/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.io.Serializable;
import java.util.ArrayList;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {
    
    public int docID;
    public double score;
    // Important: This MUST to be kept sorted!
    public ArrayList<Integer> offsets;

    public PostingsEntry(int docID, int offset, double score) {
        this.docID = docID;
        this.score = score;
        offsets = new ArrayList<Integer>();
        offsets.add(offset);
    }

    public Object clone() throws CloneNotSupportedException {
        PostingsEntry pe = new PostingsEntry(docID, 0, 0);
        pe.offsets = (ArrayList<Integer>) offsets.clone();
        return pe;
    }

    /**  Number of postings in this entry  */
    public int size() {
        return offsets.size();
    }

    /**  Returns the ith offset */
    public int get( int i ) {
        return offsets.get( i );
    }

    /** Adds an offset to the List of offsets, HAVE to maintain order in offsets. */
    public void add_offset(int offset) {
        int offset_index = 0;
        for(int o : offsets) {
            if(o == offset) { // This means that it already existed.
                return;
            } else if(o > offset) { // We have found the place for our offset.
                break;
            } else { // o < offset
                offset_index++;
            }
        }
        try {
            offsets.add(offset_index, offset);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("OFFSET_INDEX BROKEN");
        }
    }

    /**
     *  PostingsEntries are compared by their score (only relevant 
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
        return Double.compare( other.score, score );
    }

    public String toString() {
        return "" + docID + ":" + offsets;
    }
    
    public static ArrayList<Integer> is_followed_by(PostingsEntry a, PostingsEntry b) {
        ArrayList<Integer> result = new ArrayList<Integer>();

        int i = 0;
        int j = 0;

        while(i < a.size() && j < b.size())
        {
            int ai = a.get(i);
            int bj = b.get(j);
            if((bj - ai) == 1) {
                result.add(bj);
                i++;
                j++;
            } else if( ai >= bj) {
                j++;
            } else {
                i++;
            }
        }
        return result;
    }
}

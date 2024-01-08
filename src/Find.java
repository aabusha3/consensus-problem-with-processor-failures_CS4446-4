import java.util.Vector;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
public class Find extends Algorithm {
    private int m;                   	// Ring of identifiers has size 2^m
    private int SizeRing;              // SizeRing = 2^m
	String result = ""; 						// Locations of searched-for keys will be stored here
    public Object run() {
        return find(getID());
    }

	// Each message sent by this algorithm has the form: flag, value, ID 
	// where:
	// - if flag = "GET" then the message is a request to get the document with the given key
	// - if flag = "LOOKUP" then the message is request to forward the message to the closest
	//   processor to the position of the key
	// - if flag = "FOUND" then the message contains the key and processor that stores it
	// - if flag = "NOT_FOUND" then the requested data is not in the system
	// - if flag = "END" the algorithm terminates
	
	/* Complete method find, which must implement the Chord search algorithm using finger tables 
	   and assumming that there are two processors in the system that received the same ring identifier. */ 
	/* ----------------------------- */
    public Object find(String id) {
	/* ------------------------------ */
       try {
       
             /* The following code will determine the keys to be stored in this processor, the keys that this processor
                needs to find (if any), and the addresses of the finger table                                           */
	      Vector<Integer> searchKeys; 		// Keys that this processor needs to find in the P2P system. Only
			                               // for one processor this vector will not be empty
	      Vector<Integer> localKeys;   		// Keys stored in this processor

	      localKeys = new Vector<Integer>();
	      String[] fingerTable;                  // Addresses of the fingers are stored here
	      searchKeys = keysToFind();             // Read keys and fingers from configuration file
	      fingerTable = getKeysAndFingers(searchKeys,localKeys,id);  // Determine local keys, keys that need to be found, and fingers
	      m = fingerTable.length-1;
	      SizeRing = exp(2,m);

		/* Your initialization code goes here */
		  Message mssg = null, message;
		  String[] data;
		  int keyValue;                   	// Key that is being searched
		  boolean keyProcessed = false;
	      if (searchKeys.size() > 0) { 		// If this condition is true, the processor has keys that need to be found
			for(String e: fingerTable) System.out.println(e);
			  System.out.println(fingerTable.length);
			  // Get the next key to find and check if it is stored locally
			  keyValue = searchKeys.elementAt(0);
			  searchKeys.remove(0);           // Do not search for the same key twice
			  if (localKeys.contains(keyValue)) {
				  result = result + keyValue + ":" + id + " "; // Store location of key in the result
				  keyProcessed = true;
			  }
			  else {// Key was not stored locally
				  String keyNext = inSegment(hk(keyValue), hp(id), intFingerTable(fingerTable));
				  if(keyNext.equals("null")){
					  //mssg = makeMessage (id, pack("NOT_FOUND",keyValue));
					  result = result + id + ":not found ";
					  keyProcessed = true;
				  }
				  else mssg = makeMessage (keyNext, pack("LOOKUP",keyValue,id)); // ask closest finger
			  }
	      }

		   while (waitForNextRound()) {
			   if (mssg != null) {
				   send(mssg);
				   data = unpack(mssg.data());
				   if (data[0].equals("END") && searchKeys.size() == 0) return result;
			   }
			   mssg = null;
			   message = receive();
			   while (message != null) {
				   data = unpack(message.data());
				   if (data[0].equals("LOOKUP")) {
					   keyValue = stringToInteger(data[1]);

					   if (localKeys.contains(keyValue)) {//search local
						   mssg = makeMessage(data[2],pack("FOUND",data[1],id));
					   }
					   else {// Key was not in system
						   if (data[2].equals(id)) {
							   result = result + data[1] + ":not found ";
							   keyProcessed = true;
						   }
						   else{//forward
							   String keyNext = inSegment(hk(keyValue), hp(id), intFingerTable(fingerTable));
							   if(keyNext.equals("null")) mssg = makeMessage (data[2], pack("NOT_FOUND",keyValue));
							   else mssg = makeMessage (keyNext, pack("LOOKUP",keyValue,data[2])); // ask closest finger
						   }
					   }
				   }
				   else if (data[0].equals("FOUND")) {
					   result = result + data[1] + ":" + data[2] + " ";
					   keyProcessed = true;
				   }
				   else if (data[0].equals("NOT_FOUND")) {
					   result = result + data[1] + ":not found ";
					   keyProcessed = true;
				   }
				   else if (data[0].equals("END"))
					   if (searchKeys.size() > 0) return result;
					   else mssg = makeMessage(successor(),"END");
				   message = receive();
			   }

			   if (keyProcessed) { // Search for the next key
				   if (searchKeys.size() == 0)  // There are no more keys to find
					   mssg = makeMessage(successor(),"END");
				   else {
					   keyValue = searchKeys.elementAt(0);
					   searchKeys.remove(0);  // Do not search for same key twice
					   if (localKeys.contains(keyValue)) {
						   result = result + keyValue + ":" + id + " "; // Store location of key in the result
						   keyProcessed = true;
					   }
					   else {// Key was not stored locally
						   String keyNext = inSegment(hk(keyValue), hp(id), intFingerTable(fingerTable));
						   if(keyNext.equals("null")){
							   //mssg = makeMessage (id, pack("NOT_FOUND",keyValue));
							   result = result + id + ":not found ";
							   keyProcessed = true;
						   }
						   else mssg = makeMessage (keyNext, pack("LOOKUP",keyValue,id)); // ask closest finger
					   }
				   }
				   if (mssg != null) keyProcessed = false;
			   }
		   }
 
        } catch(SimulatorException e){
            System.out.println("ERROR: " + e.toString());
        }
    
        /* At this point something likely went wrong. If you do not have a result you can return null */
        return null;
    }


	private int[] intFingerTable(String[] fingerTable) throws SimulatorException{
		int [] intfingerTable = new int[fingerTable.length];
		for (int i = 0; i<fingerTable.length; ++i){
			intfingerTable[i] = stringToInteger(fingerTable[i]);
		}
		return intfingerTable;
	}


	/* Determine whether hk(value) is in (hp(ID),hp(succ)] */
	/* ---------------------------------------------------------------- */
	private String inSegment(int hashValue, int hashID, int[] fingerTable) throws SimulatorException{
		/* ----------------------------------------------------------------- */
		IntSummaryStatistics stat = Arrays.stream(fingerTable).summaryStatistics();
		int max = stat.getMax();

		if(hashValue < hashID){//the key is behind the node
			if(hashValue < max){
				System.out.println(hashID+":hashValue < hashID & max:null");
				return "null";
			}
			else{
				System.out.println(hashID+":hashValue < hashID:"+max);
				return integerToString(max);
			}
		}

		else if(hashValue > hashID){//the key is in front of the node
			int distance = Integer.MAX_VALUE;
			int id = -1;
			for(int i = 0; i < fingerTable.length-1; i++){
				int idistance = Math.abs(fingerTable[i] - hashValue);
				if(idistance<distance && fingerTable[i]>hashValue){
					id = i;
					distance = idistance;
				}
			}
			if(id<0){
				System.out.println(hashID+":hashValue > hashID max:"+max);
				return integerToString(max);
			}
			else{
				System.out.println(hashID+":hashValue > hashID:"+fingerTable[id]);
				return integerToString(fingerTable[id]);
			}
		}

		else return "null";
	}

	/* Determine the keys that need to be stored locally and the keys that the processor needs to find.
	   Negative keys returned by the simulator's method keysToFind() are to be stored locally in this 
           processor as positive numbers.                                                                    */
	/* ---------------------------------------------------------------------------------------------------- */
	private String[] getKeysAndFingers (Vector<Integer> searchKeys, Vector<Integer> localKeys, String id) throws SimulatorException {
	/* ---------------------------------------------------------------------------------------------------- */
		Vector<Integer>fingers = new Vector<Integer>();
		String[] fingerTable;
		String local = "";
		int m;
			
		if (searchKeys.size() > 0) {
			for (int i = 0; i < searchKeys.size();) {
				if (searchKeys.elementAt(i) < 0) {   	// Negative keys are the keys that must be stored locally
					localKeys.add(-searchKeys.elementAt(i));
					searchKeys.remove(i);
				}
				else if (searchKeys.elementAt(i) > 1000) {
					fingers.add(searchKeys.elementAt(i)-1000);
					searchKeys.remove(i);
				}
				else ++i;  // Key that needs to be searched for
			}
		}
			
		m = fingers.size();
		// Store the finger table in an array of Strings
		fingerTable = new String[m+1];
		for (int i = 0; i < m; ++i) fingerTable[i] = integerToString(fingers.elementAt(i));
		fingerTable[m] = id;
	
		for (int i = 0; i < localKeys.size(); ++i) local = local + localKeys.elementAt(i) + " ";
		showMessage(local); // Show in the simulator the keys stored in this processor
		return fingerTable;
	}

    /* Hash function to map processor ids to ring identifiers. */
    /* ------------------------------- */
    private int hp(String ID) throws SimulatorException{
	/* ------------------------------- */
        return stringToInteger(ID) % SizeRing;
    }

    /* Hash function to map keys to ring identifiers */
    /* ------------------------------- */
    private int hk(int key) {
    /* ------------------------------- */
        return key % SizeRing;
    }

    /* Compute base^exponent ("base" to the power "exponent") */
    /* --------------------------------------- */
    private int exp(int base, int exponent) {
    /* --------------------------------------- */
        int i = 0;
        int result = 1;

		while (i < exponent) {
			result = result * base;
			++i;
		}
		return result;
    }
}

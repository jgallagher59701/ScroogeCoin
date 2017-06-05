
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;

//import Transaction.Input;

public class TxHandler {

	// The Block Chain is the ledger
	// TODO Maybe the ledger is the UTXO Pool?
	// FIXME Not needed. private ArrayList<Transaction> blockChain;
	
	// These output transactions are the Create Coin transaction
	private UTXOPool initialUtxoPool;
	
	// The current UTXO Pool
	private UTXOPool currentUtxoPool;
	
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     * 
     * @note Assume the initial UTXOPool is not empty and that it is effectively the CreateCoin
     * transaction.
     */
    public TxHandler(UTXOPool utxoPool) {
    	initialUtxoPool = new UTXOPool(utxoPool);	// Copy the initial UTXO Pool
    	currentUtxoPool = new UTXOPool(utxoPool);	// And set the initial value of the current UTXO pool
    	
    	/*
    	  FIXME Not needed.
    	  
    	blockChain = new ArrayList<Transaction>();	// Init as empty
    	
    	// The Create Coin transaction
    	Transaction initialBlock = new Transaction();
    	
    	// Load the initial utxo pool into the block chain as the Transaction block.
    	// The initial block in the Block Chain (BC) consists of output transactions
    	// only. Extract these from the initial UTXO pool.
    	ArrayList<UTXO> utxos = initialUtxoPool.getAllUTXO();
    	for (UTXO outputTx : utxos) {
    		Transaction.Output output = initialUtxoPool.getTxOutput(outputTx);
    		initialBlock.addOutput(output.value, output.address);
    	}
    	
    	// Compute the hash and add it to the block
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(initialBlock.getRawTx());
            initialBlock.setHash(md.digest());
        } catch (NoSuchAlgorithmException x) {
            x.printStackTrace(System.err);
        }

    	// Add the block to the chain
    	blockChain.add(initialBlock);
    	*/
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // The 'claimed outputs' (inputs) to this transaction; used to ensure no UTXO is claimed twice
    	ArrayList<UTXO> txInputs = new ArrayList<UTXO>();
    	
    	// holds the sum of the input values
    	double totInputVal = 0.0;
    	for (int i = 0; i < tx.numInputs(); ++i) {
    		// #1  all outputs claimed by the Transaction (i.e., the inputs it contains) are in the current UTXO pool
    		Transaction.Input in = tx.getInput(i);
    		UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
    		if (!currentUtxoPool.contains(u)) {
    			return false;
    		}
    		
    		// #2 The signature is valid
    		Transaction.Output out = currentUtxoPool.getTxOutput(u);
    		if (!Crypto.verifySignature(out.address /*pubKey*/, tx.getRawDataToSign(i) /*message*/, in.signature))
    			return false;
    		
    		// #3 No UTXO is claimed more than once by the tx inputs
    		if (txInputs.contains(u))
    			return false;
    		else
    			txInputs.add(u);
    		
    		// #5 Part one: Sum the value of the inputs.
    		// Look in the current UTXO pool for in.prevTxHash and use the
    		// value of the matching Output as the value of the input.
    		totInputVal += currentUtxoPool.getTxOutput(u).value;
    	}
    	
    	// #4 all of {@code tx}s output values are non-negative, and
    	// The sum of the output values
    	double totOutputVal = 0.0;
    	for (int i = 0; i < tx.numOutputs(); ++i) {
    		if (tx.getOutput(i).value < 0.0)
    			return false;
    		
    		// #5 Part two: Sum the value of the outputs
    		totOutputVal += tx.getOutput(i).value;
    	}
    	
    	// #5 the sum of {@code tx}s input values is greater than or equal to the sum of its output values
    	if (totInputVal < totOutputVal)
    		return false;
    	
    	return true;	// woo hoo
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    	// Find the valid transactions
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        for (Transaction tx: possibleTxs) {
    		if (isValidTx(tx))
    			validTxs.add(tx);
    	}
    	    	
    	ArrayList<Transaction> invalidTxList = new ArrayList<Transaction>();
    	UTXOPool tmpPool = new UTXOPool(currentUtxoPool);
    	
    	for (Transaction tx: validTxs) {
    		// Modify so a Transaction found to be invalid causes no change to the currentUtxoPool
    		for (int output = 0; output < tx.numOutputs(); ++output) {
    			tmpPool.addUTXO(new UTXO(tx.getHash(), output), tx.getOutput(output));
    		}
    	}
    	
    	for (Transaction tx: validTxs) {
    		// Now remove the Outputs in the current pool that are 'spent' by an Input.
    		// If an Input spends an Output that doesn't exist, the transaction is invalid
    		boolean validTx = true;	// Assume they are OK
    		for (int input = 0; input < tx.numInputs() && validTx; ++input) {
    			UTXO u = new  UTXO(tx.getInput(input).prevTxHash, tx.getInput(input).outputIndex);
    			if (tmpPool.contains(u)) {
    				tmpPool.removeUTXO(u);
    			}
    			else {
    				validTx = false;
    			}
     		}
    		
    		if (!validTx)
    			invalidTxList.add(tx);
    	}
    	
    	currentUtxoPool = tmpPool;
    	
        for (Transaction invalid : invalidTxList) {
            validTxs.remove(invalid);
        }

    	return validTxs.toArray(new Transaction[validTxs.size()]);
    }
}

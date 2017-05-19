
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;

//import Transaction.Input;

public class TxHandler {

	// The Block Chain is the ledger
	// TODO Maybe the ledger is the UTXO Pool?
	private ArrayList<Transaction> blockChain;
	
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
        // IMPLEMENT THIS
    	// #1  all outputs claimed by the Transaction (i.e., the inputs it contains) are in the current UTXO pool
    	//for (Transaction.Input in : tx.getInputs()) {
    	for (int i = 0; i < tx.numInputs(); ++i) {
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
    	}
    	
    	
    	return false;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
    	return null;
    }

}

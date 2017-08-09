import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        // This is just a constructor where the utxPool parameter is passed by another parts
        // of the scroogecoin implementation. We assume utxpool has the collection of UTXO.
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        double sum = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                return false;
            }
            // track the sum of the output for rule 5.
            sum = sum + output.value;
        }
        double in_sum = 0;
        // index
        ArrayList<UTXO> utxoList = new ArrayList<UTXO>();
        int current_index = 0;
        for (Transaction.Input input : tx.getInputs()) {
            // Create a UTXO out of the current, in-progress, transaction.
            // This will be used to determine if this transactions will go ahead.
            UTXO unspentTransactionOutput = new UTXO(input.prevTxHash, input.outputIndex);
            // (1) all outputs claimed by {@code tx} are in the current UTXO pool,
            if (!this.utxoPool.contains(unspentTransactionOutput)) {
                return false;
            }

            //(2) the signatures on each input of {@code tx} are valid,
            //   public static boolean verifySignature(PublicKey pubKey, byte[] message, byte[] signature)
            if (!Crypto.verifySignature(
                    utxoPool.getTxOutput(unspentTransactionOutput).address,
                    tx.getRawDataToSign(current_index),
                    input.signature)
                    ) {
                return false;
            }


            // (3) no UTXO is claimed multiple times by {@code tx},
            if (utxoList.contains(unspentTransactionOutput)) {
                return false;
            }
            utxoList.add(unspentTransactionOutput);

             in_sum += utxoPool.getTxOutput(unspentTransactionOutput).value;
            // keep at the end
            current_index++;

        }
        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
        if (in_sum < sum) {
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        Transaction validTransactions[] = new Transaction[possibleTxs.length];

        for (int i =0; i < possibleTxs.length; i++) {
            if(this.isValidTx(possibleTxs[i])) {
                Transaction valid = possibleTxs[i];
                validTransactions[i] = valid;
                int outputIndex = 0;
                for (Transaction.Output output : valid.getOutputs()) {
                    UTXO ourUTXO = new UTXO(valid.getHash(),outputIndex);
                    // Update the utxo pool
                    utxoPool.addUTXO(ourUTXO, output);
                    outputIndex++;
                }

                for (Transaction.Input input : valid.getInputs()) {
                    // Get UTXO for current transaction
                    UTXO currInUTXO = new UTXO(input.prevTxHash,input.outputIndex);
                    //remove spent UTXO from UTXO pool
                    utxoPool.removeUTXO(currInUTXO);

                }
            }
        }
        // normalize the transactions
        int size = 0;
        for (int i =0; i < validTransactions.length; i++) {
            if (validTransactions[i]  != null) {
                size++;
            }
        }

        Transaction finalValidTransactions[] = new Transaction[size];
        for (int i =0; i < validTransactions.length; i++) {
            if (validTransactions[i]  != null) {
                finalValidTransactions[i] = validTransactions[i];
            }
        }

        return finalValidTransactions;
    }

}

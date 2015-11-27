
package sample;

        import java.io.*;
        import java.util.concurrent.BlockingQueue;
        import java.util.concurrent.atomic.AtomicBoolean;

        import java.io.BufferedReader;


public class BluetoothModelReceiver extends Thread{

    private BlockingQueue<Double> queue;
    private AtomicBoolean fileIsFinished;
    private String filePath;

    BluetoothModelReceiver(BlockingQueue<Double> queue, String filePath, AtomicBoolean fileIsFinished) {
        this.queue = queue;
        this.filePath = filePath;
        this.fileIsFinished = fileIsFinished;
    }

    private void addElement(Double el) {
        queue.add(el);
    }


    @Override
    public void run() {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String s;
        try {
            assert bufferedReader != null;
            while ((s = bufferedReader.readLine()) != null) {
                addElement(Double.valueOf(s));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileIsFinished.set(true);
    }

}
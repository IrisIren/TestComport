package sample;

import gnu.io.NRSerialPort;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class ComportInputData implements Runnable {
    NRSerialPort serialPort = new NRSerialPort("COM3", 115200);

    public ComportInputData() {
        if(serialPort.connect()){
            System.out.println(" Port opened");
        }
    }

    @Override
    public void run() {
        try {
            while(serialPort.getInputStream().available() > 0){
                int data = serialPort.getInputStream().read();
                System.out.print("" + data + "  ");
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) {
        ComportInputData cd = new ComportInputData();
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(cd, 0L, 10L, TimeUnit.MILLISECONDS);
    }
}

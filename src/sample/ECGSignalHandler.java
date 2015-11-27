
package sample;

        import java.util.ArrayList;
        import java.util.List;
        import java.util.NavigableMap;
        import java.util.TreeMap;
        import java.util.concurrent.BlockingQueue;
        import java.util.concurrent.ConcurrentLinkedQueue;
        import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("ALL")
public class ECGSignalHandler extends Thread {

    private BlockingQueue<Double> queue;
    private AtomicBoolean fileIsFinished;

    private double SPKI = 0;
    private double NPKI = 0;
    private double THRESHOLD1 = 0;

    private final int integrationWindowWidth = 50;

    private List<Double> signal = new ArrayList<Double>();
    private ConcurrentLinkedQueue<Double> GUIQueue;
    private ConcurrentLinkedQueue<Double> GUIQueue1;
    private List<Double> lowPassed = new ArrayList<Double>();
    private List<Double> highPassed = new ArrayList<Double>();
    private List<Double> derivative = new ArrayList<Double>();
    private List<Double> squaring = new ArrayList<Double>();
    private List<Double> movingWindowIntegration = new ArrayList<Double>();
    public  List<Double> RRPeaks = new ArrayList<Double>();
    private List<Integer> RRPeaksIndex = new ArrayList<Integer>();

    private NavigableMap<Integer, Double> QRSPeaks = new TreeMap<Integer, Double>();
    private List<Integer> QRSPeakCoordinates = new ArrayList<Integer>();

    private Double p = 0.0;
    ECGSignalHandler(BlockingQueue<Double> queue, ConcurrentLinkedQueue<Double> GUIQueue, ConcurrentLinkedQueue<Double> GUIQueue1, AtomicBoolean fileIsFinished) {
        this.GUIQueue = GUIQueue;
        this.GUIQueue1 = GUIQueue1;
        this.queue = queue;
        this.fileIsFinished = fileIsFinished;
    }

    @Override
    public void run() {
        setting();
        while (!fileIsFinished.get() || !queue.isEmpty()) {
            otherPeaks();
        }
        System.out.println("Peaks:");
        for (Integer index : QRSPeaks.keySet()) {
            System.out.println("  " + index + " -> " + QRSPeaks.get(index));
        }

        for (int i = 1; i < RRPeaks.size(); i++) {
            System.out.println("List with peaks and zeros:" + RRPeaks.get(i));
        }

        for (Integer integer : QRSPeaks.keySet()) {
            QRSPeakCoordinates.add(integer);
        }
        double medium = 0;
        if (QRSPeakCoordinates.size() > 1) {
            for (int i = 1; i < QRSPeakCoordinates.size(); i++) {
                System.out.println("Pulse[" + (i - 1) + "]: " + Math.round(60 / ((QRSPeakCoordinates.get(i) - QRSPeakCoordinates.get(i - 1)) * 0.002)));
                medium += (QRSPeakCoordinates.get(i) - QRSPeakCoordinates.get(i - 1)) * 0.002;
            }
            System.out.println("ЧСС на всем интервале измерений" + "  =  " + Math.round(60 * (QRSPeakCoordinates.size() - 1) / medium) + " " + "уд/мин");
        }
    }

    private void lowpassFilter() {
        int lastSignalIndex = signal.size() - 1;
        int lastLPIndex = lowPassed.size() - 1;
        Double value0 = 0.0;
        Double value1 = 0.0;
        Double value4 = 0.0;
        Double value3 = 0.0;

        if (lastLPIndex >= 0)
            value0 = 2 * lowPassed.get(lastLPIndex);
        if (lastLPIndex >= 1)
            value1 = -lowPassed.get(lastLPIndex - 1);
        Double value2 = signal.get(lastSignalIndex);
        if (lastSignalIndex >= 6)
            value3 = -2 * signal.get(lastSignalIndex - 6);
        if (lastSignalIndex >= 12)
            value4 = signal.get(lastSignalIndex - 12);

        lowPassed.add((value0 + value1) / 32 + value3 + value4 + value2);
    }

    private void highpassFilter() {
        int lastLPIndex = lowPassed.size() - 1;
        int lastHPIndex = highPassed.size() - 1;

        Double value0 = 0.0;
        Double value4 = 0.0;
        Double value1 = 0.0;
        Double value2 = 0.0;
        Double value3 = 0.0;
        if (lastHPIndex >= 0)
            value0 = highPassed.get(lastHPIndex);
        if (lastLPIndex >= 0)
            value4 = -(lowPassed.get(lastLPIndex) / 32);
        if (lastLPIndex >= 16)
            value1 = lowPassed.get(lastLPIndex - 16);
        if (lastLPIndex >= 17)
            value2 = -lowPassed.get(lastLPIndex - 17);
        if (lastLPIndex >= 32)
            value3 = -(lowPassed.get(lastLPIndex - 32) / 32);
        highPassed.add(value0 + value1 + value2 + value3 + value4);
    }

    private void derivativeFilter() {
        int lastHPIndex = highPassed.size() - 1;
        Double value0 = 0.0;
        Double value1 = 0.0;
        Double value2 = 0.0;
        Double value3 = 0.0;
        if (lastHPIndex >= 0)
            value0 = 2 * (highPassed.get(lastHPIndex));
        if (lastHPIndex >= 1)
            value1 = highPassed.get(lastHPIndex - 1);
        if (lastHPIndex >= 2)
            value2 = -highPassed.get(lastHPIndex - 2);
        if (lastHPIndex >= 1)
            value3 = -2 * highPassed.get(lastHPIndex - 1);

        derivative.add((value0 + value1 + value2 + value3) / 8.0);
    }

    private void squaringFilter() {
        int n = derivative.size() - 1;
        squaring.add(Math.pow(derivative.get(n), 2));
    }

    private void movingWindowIntegralFilter() {
        int n = squaring.size() - 1;
        Double sum = 0.0;
        if (n >= integrationWindowWidth) {
            for (int i = n; i >= n - (integrationWindowWidth - 1); i--) {
                sum += squaring.get(i);
            }
            sum /= integrationWindowWidth;
        }
        movingWindowIntegration.add(sum);
    }

    private void stepping() {
        if (queue.isEmpty())
            return;
        try {
            Double d = queue.take();
            signal.add(d);
            GUIQueue.add(d);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        lowpassFilter();
        highpassFilter();
        derivativeFilter();
        squaringFilter();
        movingWindowIntegralFilter();
    }

    private void setting() {
        double sample = 500;
        while ((movingWindowIntegration.size() < sample) && (!fileIsFinished.get() || !queue.isEmpty())) {
            stepping();
        }
        double firstQRSPeak = 0;
        int firstQRSPeakIndex = 0;
        for (int i = 0; i < sample; i++) {
            if (movingWindowIntegration.get(i) > firstQRSPeak) {
                firstQRSPeak = movingWindowIntegration.get(i);
                firstQRSPeakIndex = i;
                Double p = i*1.0;
            }
            GUIQueue1.add(p);
        }

        for (int i = firstQRSPeakIndex - integrationWindowWidth; i < firstQRSPeakIndex; i++) {
            if ((signal.get(i - 1) > signal.get(i - 2)) && (signal.get(i) < signal.get(i - 1))) {
                QRSPeaks.put(i, signal.get(i - 1));
                RRPeaksIndex.add(signal.size() - 2);
                Double p = i*1.0;
                GUIQueue1.add(p);
                break;
            }
        }

        SPKI = firstQRSPeak;
        NPKI = firstQRSPeak / 2;
        THRESHOLD1 = NPKI + 0.25 * (SPKI - NPKI);
        System.out.println("  Thresholds:");
        System.out.println("    SPKI = " + SPKI);
        System.out.println("    NPKI = " + NPKI);
        System.out.println("    THRESHOLD I1 = " + THRESHOLD1);
    }

    private void otherPeaks() {
        stepping();
        int n = movingWindowIntegration.size() - 1;
        double current = movingWindowIntegration.get(n);
        double previous = movingWindowIntegration.get(n - 1);
        double prePrevious = movingWindowIntegration.get(n - 2);

        if ((prePrevious < previous) && (previous > current)) {
            int peakIndex = n - 1;
            double PEAKI = previous;

            if (PEAKI >= THRESHOLD1) {
                SPKI = 0.125 * PEAKI + 0.875 * SPKI;

                for (int i = peakIndex - integrationWindowWidth; i < peakIndex; i++) {
                    if ((signal.get(i - 1) > signal.get(i - 2)) && (signal.get(i) < signal.get(i - 1)) && (signal.get(i - 1) >  Math.sqrt(THRESHOLD1))) {
                        QRSPeaks.put(i, signal.get(i - 1));
                        Double p = i*1.0;
                        GUIQueue1.add(p);
                        break;
                    }
                }
            } else {
                NPKI = 0.125 * PEAKI + 0.875 * NPKI;
            }
            THRESHOLD1 = NPKI + 0.25 * (SPKI - NPKI);
        }

    }
}

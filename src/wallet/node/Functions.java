package wallet.node;

import wallet.PolynomialRegression;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * Created by Itai on 10/03/2018.
 */
public class Functions {


    static void broadcast(Message broadcastMessage, DatagramSocket socket ) {
        List<InetAddress> broadcastList = null;

        try {
            broadcastList = listAllBroadcastAddresses();

            if (broadcastList != null && broadcastList.size() > 0) {
                InetAddress address = broadcastList.get(0);
                socket = new DatagramSocket();
                socket.setBroadcast(true);

                byte[] buffer = broadcastMessage.toString().getBytes();

                DatagramPacket packet
                        = new DatagramPacket(buffer, 0, buffer.length, address, 4445);

                socket.send(packet);
                //   socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static List<InetAddress> listAllBroadcastAddresses() throws SocketException {
        List<InetAddress> broadcastList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces
                = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            networkInterface.getInterfaceAddresses().stream()
                    .map(InterfaceAddress::getBroadcast)
                    .filter(Objects::nonNull)
                    .forEach(broadcastList::add);
        }
        return broadcastList;
    }




     static String[] interpolate(String[] vals, int f, boolean isRobust, boolean returnNullIfFails) {

        double[] y = new double[vals.length];
        for (int i = 0; i < vals.length; i++) {
            y[i] = Double.parseDouble(vals[i]);
        }
/*        if (mNumber == 1 && confirmValuesThread == null)         // Fuck node 1
            y[1] += 2.0;*/
        //        y[y.length-1] = y[y.length-1] *3;
        double[] x = new double[vals.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i + 1;
        }
   //     y[0] = 300;

         boolean condition ;
        if(isRobust){
            condition = combine(x,y,f+1);
        }else {
            condition = (new PolynomialRegression(x, y, f)).R2() == 1.0;
        }
        if (!condition) {
            if(returnNullIfFails)
                return null;
            for (int i = 0; i < vals.length; i++) {
                vals[i] = "0";
            }
       //     System.out.println("bad polynomial");
        } else {
          //  System.out.println("good polynomial");
        }
        return vals;
    }

    private static boolean combine(double[] arrX,double[] arrY, int r) {
        double[] resX = new double[r];
        double[] resY = new double[r];
        return doCombine(arrX, resX,arrY,resY, 0, 0, r);
    }

    private static boolean doCombine(double[] arrX, double[] resX,double[] arrY, double[] resY, int currIndex, int level, int r) {
        if(level == r){
            return printArray(resX, resY, r);
        }
        for (int i = currIndex; i < arrX.length; i++) {
            resX[level] = arrX[i];
            resY[level] = arrY[i];
            if (doCombine(arrX, resX,arrY,resY, i+1, level+1, r))
                return true;
            //way to avoid printing duplicates
            if(i < arrX.length-1 && arrX[i] == arrX[i+1]){
                i++;
            }
        }
        return false;
    }

    private static boolean printArray(double[] resX, double[] resY, int f) {
       return (new PolynomialRegression(resX, resY, f).R2() == 1.0);
    }


    /**
     * Calculates the first prime number that is greater than 10*f
     *
     * @return prime number that is greater than 10*f
     */
    public static int generatePrime(int f) {
        boolean isPrime = true;
        int n = (15 * f);
        do {
            isPrime = true;
            n++;
            for (long factor = 2; factor * factor <= n; factor++) {

                // if factor divides evenly into n, n is not prime, so break out of loop
                if (n % factor == 0) {
                    isPrime = false;
                    break;
                }
            }
        } while (!isPrime);
        return n;
    }

}

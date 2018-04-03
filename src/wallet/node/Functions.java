package wallet.node;

import wallet.PolynomialRegression;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Itai on 10/03/2018.
 */
public class Functions {


    /**
     * After interpolation success - predicts the value of nodeNumber at the polynomial
     *
     * @param vals       - Y values (x values are the indecies)
     * @param f          - The degree of the polynomial
     * @param nodeNumber - The x value to predict it's y value
     * @return - P(nodeNumber)
     */
    static double predict(String[] vals, int f, int nodeNumber) {
        double[] y = new double[vals.length];
        for (int i = 0; i < vals.length; i++) {
            y[i] = Double.parseDouble(vals[i]);
        }
        double[] x = new double[vals.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i + 1;
        }
        PolynomialRegression p = new PolynomialRegression(x, y, f);
        if (p.R2() == 1.0)
            return p.predict(nodeNumber);
        return 0;
    }

    /**
     * This functions tries to interpolate according to the input value even if there are faulty values
     *
     * @param vals               - Y values (x values are the indecies)
     * @param f                  - The degree of the polynomial
     * @param value              - The x value to predict it's y value
     * @param numOfCorrectValues - The polynomial must agree with at least this number of values
     * @return - P(value)
     */
    static Double interpolateRobust(String[] vals, int f, int value, int numOfCorrectValues) {
        ArrayList<Double> yVals = new ArrayList<>();
        ArrayList<Double> xVals = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            try {
                if (vals[i] != null && !vals[i].isEmpty()) {
                    yVals.add(Double.parseDouble(vals[i]));
                    xVals.add((double) (i + 1));
                }
            } catch (NullPointerException e) {
                System.out.println("bad value = " + vals[i]);
            }
        }
        PolynomialRegression p = combine(xVals, yVals, f + 1, numOfCorrectValues);
        if (p != null) {
            return p.predict(value);
        }
        return null;
    }

    /**
     * Translates string array to double array and then attempts to interpolate the points
     *
     * @param vals              - Y values (x values are the indecies)
     * @param f                 - The degree of the polynomial
     * @param returnNullIfFails - If true returns null - else put zeros
     * @return - If the interpolation success - returns the input array, else zeros/null
     */
    static String[] interpolate(String[] vals, int f, boolean returnNullIfFails) {
        double[] y = new double[vals.length];
        for (int i = 0; i < vals.length; i++) {
            y[i] = Double.parseDouble(vals[i]);
        }
        double[] x = new double[vals.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i + 1;
        }
        boolean condition = (new PolynomialRegression(x, y, f)).R2() == 1.0;

        if (!condition) {
            if (returnNullIfFails)
                return null;
            for (int i = 0; i < vals.length; i++) {
                vals[i] = "0";
            }
        }
        return vals;
    }

    private static PolynomialRegression combine(ArrayList<Double> arrX, ArrayList<Double> arrY, int r, int numOfOkPoints) {
        double[] resX = new double[r];
        double[] resY = new double[r];
        return doCombine(arrX, resX, arrY, resY, 0, 0, r, numOfOkPoints);
    }

    private static PolynomialRegression doCombine(ArrayList<Double> arrX, double[] resX, ArrayList<Double> arrY, double[] resY, int currIndex, int level, int r, int numOfOkPoints) {
        if (level == r) {
            return areValuesInterpolate(resX, resY, r, arrX, arrY, numOfOkPoints);
        }
        for (int i = currIndex; i < arrX.size(); i++) {
            resX[level] = arrX.get(i);
            resY[level] = arrY.get(i);
            PolynomialRegression p = doCombine(arrX, resX, arrY, resY, i + 1, level + 1, r, numOfOkPoints);
            if (p != null)
                return p;
            //way to avoid printing duplicates
            if (i < arrX.size() - 1 && arrX.get(i).equals(arrX.get(i + 1))) {
                i++;
            }
        }
        return null;
    }

    private static PolynomialRegression areValuesInterpolate(double[] resX, double[] resY, int f, ArrayList<Double> arrX, ArrayList<Double> arrY, int numOfOkPoints) {
        PolynomialRegression p = new PolynomialRegression(resX, resY, f);
        if (p.R2() == 1.0 && checkCorrectnessOfPolynomial(p, arrX, arrY, numOfOkPoints)) {
            return p;
        }
        return null;
    }

    private static boolean checkCorrectnessOfPolynomial(PolynomialRegression p, ArrayList<Double> arrX, ArrayList<Double> arrY, int numOfOkPoints) {
        int counter = 0;
        for (int i = 0; i < arrX.size(); i++) {
            if (Math.round(p.predict(arrX.get(i))) == Math.round(arrY.get(i))) {
                counter++;
            }
        }
        return counter >= numOfOkPoints;
    }

    /**
     * Calculates the first prime number that is greater than 15*f
     *
     * @return prime number that is greater than 15*f
     */
    static int generatePrime(int f) {
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


    /**
     * Computes the polynomial arr[0]*x^0 + .. + arr[f]*x^f
     *
     * @param arr - array of coefficients
     * @param x   - the value to calculate with
     * @return - The value of the polynomial in x.
     */
    public static long computePolynomial(int[] arr, int x) {
        long res = 0;
        for (int i = 0; i < arr.length; i++) {
            res += arr[i] * Math.pow(x, i);
        }
        return res;
    }

    /**
     * Randomly creates array of coefficients to simulate a polynomial
     *
     * @return - Array of coefficients
     */
    public static int[] createArrayOfCoefs(int f, int boundForRandom, Random random) {
        int[] arr = new int[f + 1];
        for (int i = 1; i < arr.length; i++) {
            arr[i] = random.nextInt(boundForRandom);
            if (arr[i] == 0)
                arr[i]++;
        }
        return arr;
    }

}

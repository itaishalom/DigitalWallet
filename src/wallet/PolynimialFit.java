/*
package wallet;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.dense.row.linsol.AdjustableLinearSolver_DDRM;

*/
/**
 * Created by Itai on 04/03/2018.
 *//*

public class PolynimialFit {

    // Vandermonde matrix
    DMatrixRMaj A;
    // matrix containing computed polynomial coefficients
    DMatrixRMaj coef;
    // observation matrix
    DMatrixRMaj y;

    // solver used to compute
    AdjustableLinearSolver_DDRM solver;

    */
/**
     * Constructor.
     *
     * @param degree The polynomial's degree which is to be fit to the observations.
     *//*

    public PolynimialFit(int degree) {
        coef = new DMatrixRMaj(degree + 1, 1);
        A = new DMatrixRMaj(1, degree + 1);
        y = new DMatrixRMaj(1, 1);

        // create a solver that allows elements to be added or removed efficiently
        solver = LinearSolverFactory_DDRM.adjustable();
    }

    */
/**
     * Returns the computed coefficients
     *
     * @return polynomial coefficients that best fit the data.
     *//*

    public double[] getCoef() {
        return coef.data;
    }

    */
/**
     * Computes the best fit set of polynomial coefficients to the provided observations.
     *
     * @param samplePoints where the observations were sampled.
     * @param observations A set of observations.
     *//*

    public void fit(double samplePoints[], double[] observations) {
        // Create a copy of the observations and put it into a matrix
        y.reshape(observations.length, 1, false);
        System.arraycopy(observations, 0, y.data, 0, observations.length);

        // reshape the matrix to avoid unnecessarily declaring new memory
        // save values is set to false since its old values don't matter
        A.reshape(y.numRows, coef.numRows, false);

        // set up the A matrix
        for (int i = 0; i < observations.length; i++) {

            double obs = 1;

            for (int j = 0; j < coef.numRows; j++) {
                A.set(i, j, obs);
                obs *= samplePoints[i];
            }
        }

        // process the A matrix and see if it failed
        if (!solver.setA(A))
            throw new RuntimeException("Solver failed");

        // solver the the coefficients
        solver.solve(y, coef);
    }

    */
/**
     * Removes the observation that fits the model the worst and recomputes the coefficients.
     * This is done efficiently by using an adjustable solver.  Often times the elements with
     * the largest errors are outliers and not part of the system being modeled.  By removing them
     * a more accurate set of coefficients can be computed.
     *//*

    public void removeWorstFit() {
        // find the observation with the most error
        int worstIndex = -1;
        double worstError = -1;

        for (int i = 0; i < y.numRows; i++) {
            double predictedObs = 0;

            for (int j = 0; j < coef.numRows; j++) {
                predictedObs += A.get(i, j) * coef.get(j, 0);
            }

            double error = Math.abs(predictedObs - y.get(i, 0));

            if (error > worstError) {
                worstError = error;
                worstIndex = i;
            }
        }

        // nothing left to remove, so just return
        if (worstIndex == -1)
            return;

        // remove that observation
        removeObservation(worstIndex);

        // update A
        solver.removeRowFromA(worstIndex);

        // solve for the parameters again
        solver.solve(y, coef);
    }

    */
/**
     * Removes an element from the observation matrix.
     *
     * @param index which element is to be removed
     *//*

    private void removeObservation(int index) {
        final int N = y.numRows - 1;
        final double d[] = y.data;

        // shift
        for (int i = index; i < N; i++) {
            d[i] = d[i + 1];
        }
        y.numRows--;
    }
}*/

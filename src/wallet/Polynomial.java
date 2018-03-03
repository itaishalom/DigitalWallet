package wallet;

/**
 * Created by Itai on 03/03/2018.
 */
/******************************************************************************
 *  Compilation:  javac Polynomial.java
 *  Execution:    java Polynomial
 *
 *  Polynomials with integer coefficients.
 *
 *  % java Polynomial
 *  zero(x)     = 0
 *  p(x)        = 4x^3 + 3x^2 + 2x + 1
 *  q(x)        = 3x^2 + 5
 *  p(x) + q(x) = 4x^3 + 6x^2 + 2x + 6
 *  p(x) * q(x) = 12x^5 + 9x^4 + 26x^3 + 18x^2 + 10x + 5
 *  p(q(x))     = 108x^6 + 567x^4 + 996x^2 + 586
 *  p(x) - p(x) = 0
 *  0 - p(x)    = -4x^3 - 3x^2 - 2x - 1
 *  p(3)        = 142
 *  p'(x)       = 12x^2 + 6x + 2
 *  p''(x)      = 24x + 6
 *
 ******************************************************************************/

/**
 *  The {@code Polynomial} class represents a polynomial with integer
 *  coefficients.
 *  Polynomials are immutable: their values cannot be changed after they
 *  are created.
 *  It includes methods for addition, subtraction, multiplication, composition,
 *  differentiation, and evaluation.
 *  <p>
 *  For additional documentation,
 *  see <a href="https://algs4.cs.princeton.edu/99scientific">Section 9.9</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public class Polynomial {
    private int[] mCoef;   // coefficients p(x) = sum { coef[i] * x^i }
    private int degree;   // degree of polynomial (-1 for the zero polynomial)
    private String mVar;

    public Polynomial(int[] coefs, String var) {
        mCoef = coefs;
        mVar = var;
        reduce();
    }

    /**
     * Initializes a new polynomial a x^b
     * @param a the leading coefficient
     * @param b the exponent
     * @throws IllegalArgumentException if {@code b} is negative
     */
    public Polynomial(int a, int b, String var) {
        if (b < 0) {
            throw new IllegalArgumentException("exponent cannot be negative: " + b);
        }
        mCoef = new int[b + 1];
        mCoef[b] = a;
        mVar = var;
        reduce();
    }

    // pre-compute the degree of the polynomial, in case of leading zero coefficients
    // (that is, the length of the array need not relate to the degree of the polynomial)
    private void reduce() {
        degree = -1;
        for (int i = mCoef.length - 1; i >= 0; i--) {
            if (mCoef[i] != 0) {
                degree = i;
                return;
            }
        }
    }

    /**
     * Returns the degree of this polynomial.
     *
     * @return the degree of this polynomial, -1 for the zero polynomial.
     */
    public int degree() {
        return degree;
    }

    /**
     * Returns the sum of this polynomial and the specified polynomial.
     *
     * @param that the other polynomial
     * @return the polynomial whose value is {@code (this(x) + that(x))}
     */
    public Polynomial plus(Polynomial that) {
        Polynomial poly = new Polynomial(0, Math.max(this.degree, that.degree),mVar);
        for (int i = 0; i <= this.degree; i++) poly.mCoef[i] += this.mCoef[i];
        for (int i = 0; i <= that.degree; i++) poly.mCoef[i] += that.mCoef[i];
        poly.reduce();
        return poly;
    }

    /**
     * Returns the result of subtracting the specified polynomial
     * from this polynomial.
     *
     * @param that the other polynomial
     * @return the polynomial whose value is {@code (this(x) - that(x))}
     */
    public Polynomial minus(Polynomial that) {
        Polynomial poly = new Polynomial(0, Math.max(this.degree, that.degree),mVar);
        for (int i = 0; i <= this.degree; i++) poly.mCoef[i] += this.mCoef[i];
        for (int i = 0; i <= that.degree; i++) poly.mCoef[i] -= that.mCoef[i];
        poly.reduce();
        return poly;
    }

    /**
     * Returns the product of this polynomial and the specified polynomial.
     * Takes time proportional to the product of the degrees.
     * (Faster algorithms are known, e.g., via FFT.)
     *
     * @param that the other polynomial
     * @return the polynomial whose value is {@code (this(x) * that(x))}
     */
    public Polynomial times(Polynomial that) {
        Polynomial poly = new Polynomial(0, this.degree + that.degree,mVar);
        for (int i = 0; i <= this.degree; i++)
            for (int j = 0; j <= that.degree; j++)
                poly.mCoef[i + j] += (this.mCoef[i] * that.mCoef[j]);
        poly.reduce();
        return poly;
    }

    /**
     * Returns the composition of this polynomial and the specified
     * polynomial.
     * Takes time proportional to the product of the degrees.
     * (Faster algorithms are known, e.g., via FFT.)
     *
     * @param that the other polynomial
     * @return the polynomial whose value is {@code (this(that(x)))}
     */
    public Polynomial compose(Polynomial that) {
        Polynomial poly = new Polynomial(0, 0,mVar);
        for (int i = this.degree; i >= 0; i--) {
            Polynomial term = new Polynomial(this.mCoef[i], 0,mVar);
            poly = term.plus(that.times(poly));
        }
        return poly;
    }


    /**
     * Compares this polynomial to the specified polynomial.
     *
     * @param other the other polynoimal
     * @return {@code true} if this polynomial equals {@code other};
     * {@code false} otherwise
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;
        Polynomial that = (Polynomial) other;
        if (this.degree != that.degree) return false;
        for (int i = this.degree; i >= 0; i--)
            if (this.mCoef[i] != that.mCoef[i]) return false;
        return true;
    }

    /**
     * Returns the result of differentiating this polynomial.
     *
     * @return the polynomial whose value is {@code this'(x)}
     */
    public Polynomial differentiate() {
        if (degree == 0) return new Polynomial(0, 0,mVar);
        Polynomial poly = new Polynomial(0, degree - 1,mVar);
        poly.degree = degree - 1;
        for (int i = 0; i < degree; i++)
            poly.mCoef[i] = (i + 1) * mCoef[i + 1];
        return poly;
    }

    /**
     * Returns the result of evaluating this polynomial at the point x.
     *
     * @param x the point at which to evaluate the polynomial
     * @return the integer whose value is {@code (this(x))}
     */
    public int evaluate(int x) {
        int p = 0;
        for (int i = degree; i >= 0; i--)
            p = mCoef[i] + (x * p);
        return p;
    }

    /**
     * Compares two polynomials by degree, breaking ties by coefficient of leading term.
     *
     * @param that the other point
     * @return the value {@code 0} if this polynomial is equal to the argument
     * polynomial (precisely when {@code equals()} returns {@code true});
     * a negative integer if this polynomialt is less than the argument
     * polynomial; and a positive integer if this polynomial is greater than the
     * argument point
     */
    public int compareTo(Polynomial that) {
        if (this.degree < that.degree) return -1;
        if (this.degree > that.degree) return +1;
        for (int i = this.degree; i >= 0; i--) {
            if (this.mCoef[i] < that.mCoef[i]) return -1;
            if (this.mCoef[i] > that.mCoef[i]) return +1;
        }
        return 0;
    }

    /**
     * Return a string representation of this polynomial.
     *
     * @return a string representation of this polynomial in the format
     * 4x^5 - 3x^2 + 11x + 5
     */
    @Override
    public String toString() {
        if (degree == -1) return "0";
        else if (degree == 0) return "" + mCoef[0];
        else if (degree == 1) return mCoef[1] + "x + " + mCoef[0];
        String s = mCoef[degree] + mVar+"^" + degree;
        for (int i = degree - 1; i >= 0; i--) {
            if (mCoef[i] == 0) continue;
            else if (mCoef[i] > 0) s = s + " + " + (mCoef[i]);
            else if (mCoef[i] < 0) s = s + " - " + (-mCoef[i]);
            if (i == 1) s = s + mVar;
            else if (i > 1) s = s + mVar+"^" + i;
        }
        return s;
    }

    public class variable{
        String mChar;
        variable(String s) {
            mChar = s;
        }
    }
}
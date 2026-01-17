package me.geyserextensionists.geyserdisplayentity.util;

public class DeltaUtils {

    public static double calculateDeltaE(double[] lab1, double[] lab2) {
        // CIEDE2000 algorithm implementation
        double deltaL = lab2[0] - lab1[0];
        double lBar = (lab1[0] + lab2[0]) / 2.0;
        double c1 = Math.sqrt(lab1[1] * lab1[1] + lab1[2] * lab1[2]);
        double c2 = Math.sqrt(lab2[1] * lab2[1] + lab2[2] * lab2[2]);
        double cBar = (c1 + c2) / 2.0;
        double a1Prime = lab1[1] + lab1[1] / 2.0 * (1 - Math.sqrt(Math.pow(cBar, 7) / (Math.pow(cBar, 7) + Math.pow(25, 7))));
        double a2Prime = lab2[1] + lab2[1] / 2.0 * (1 - Math.sqrt(Math.pow(cBar, 7) / (Math.pow(cBar, 7) + Math.pow(25, 7))));
        double c1Prime = Math.sqrt(a1Prime * a1Prime + lab1[2] * lab1[2]);
        double c2Prime = Math.sqrt(a2Prime * a2Prime + lab2[2] * lab2[2]);
        double cBarPrime = (c1Prime + c2Prime) / 2.0;
        double deltaCPrime = c2Prime - c1Prime;
        double h1Prime = Math.atan2(lab1[2], a1Prime);
        if (h1Prime < 0) h1Prime += 2 * Math.PI;
        double h2Prime = Math.atan2(lab2[2], a2Prime);
        if (h2Prime < 0) h2Prime += 2 * Math.PI;
        double deltahPrime = h2Prime - h1Prime;
        if (Math.abs(deltahPrime) > Math.PI) deltahPrime -= 2 * Math.PI * Math.signum(deltahPrime);
        double deltaHPrime = 2 * Math.sqrt(c1Prime * c2Prime) * Math.sin(deltahPrime / 2.0);
        double lBarPrime = (lab1[0] + lab2[0]) / 2.0;
        double cBarPrimeDelta = (c1Prime + c2Prime) / 2.0;
        double hBarPrime = (h1Prime + h2Prime) / 2.0;
        if (Math.abs(h1Prime - h2Prime) > Math.PI) hBarPrime -= Math.PI;
        double t = 1 - 0.17 * Math.cos(hBarPrime - Math.PI / 6) + 0.24 * Math.cos(2 * hBarPrime) + 0.32 * Math.cos(3 * hBarPrime + Math.PI / 30) - 0.20 * Math.cos(4 * hBarPrime - 63 * Math.PI / 180);
        double deltaTheta = 30 * Math.exp(-((hBarPrime - 275 * Math.PI / 180) / 25 * Math.PI / 180) * ((hBarPrime - 275 * Math.PI / 180) / 25 * Math.PI / 180));
        double rC = 2 * Math.sqrt(Math.pow(cBarPrimeDelta, 7) / (Math.pow(cBarPrimeDelta, 7) + Math.pow(25, 7)));
        double sL = 1 + (0.015 * (lBarPrime - 50) * (lBarPrime - 50)) / Math.sqrt(20 + (lBarPrime - 50) * (lBarPrime - 50));
        double sC = 1 + 0.045 * cBarPrimeDelta;
        double sH = 1 + 0.015 * cBarPrimeDelta * t;
        double rT = -Math.sin(2 * deltaTheta) * rC;
        return Math.sqrt((deltaL / sL) * (deltaL / sL) + (deltaCPrime / sC) * (deltaCPrime / sC) + (deltaHPrime / sH) * (deltaHPrime / sH) + rT * (deltaCPrime / sC) * (deltaHPrime / sH));
    }

    public static double[] rgbToLab(int r, int g, int b) {
        // Convert RGB to XYZ
        double[] xyz = rgbToXyz(r, g, b);
        // Convert XYZ to Lab
        return xyzToLab(xyz[0], xyz[1], xyz[2]);
    }

    private static double[] rgbToXyz(int r, int g, int b) {
        double var_R = (r / 255.0);
        double var_G = (g / 255.0);
        double var_B = (b / 255.0);

        if (var_R > 0.04045) var_R = Math.pow((var_R + 0.055) / 1.055, 2.4);
        else var_R = var_R / 12.92;
        if (var_G > 0.04045) var_G = Math.pow((var_G + 0.055) / 1.055, 2.4);
        else var_G = var_G / 12.92;
        if (var_B > 0.04045) var_B = Math.pow((var_B + 0.055) / 1.055, 2.4);
        else var_B = var_B / 12.92;

        var_R = var_R * 100.0;
        var_G = var_G * 100.0;
        var_B = var_B * 100.0;

        // Observer. = 2°, Illuminant = D65
        double x = var_R * 0.4124 + var_G * 0.3576 + var_B * 0.1805;
        double y = var_R * 0.2126 + var_G * 0.7152 + var_B * 0.0722;
        double z = var_R * 0.0193 + var_G * 0.1192 + var_B * 0.9505;

        return new double[]{x, y, z};
    }

    private static double[] xyzToLab(double x, double y, double z) {
        double ref_X =  95.047;
        double ref_Y = 100.000;
        double ref_Z = 108.883;

        double var_X = x / ref_X;
        double var_Y = y / ref_Y;
        double var_Z = z / ref_Z;

        if (var_X > 0.008856) var_X = Math.pow(var_X, 1.0/3.0);
        else var_X = (7.787 * var_X) + (16.0 / 116.0);
        if (var_Y > 0.008856) var_Y = Math.pow(var_Y, 1.0/3.0);
        else var_Y = (7.787 * var_Y) + (16.0 / 116.0);
        if (var_Z > 0.008856) var_Z = Math.pow(var_Z, 1.0/3.0);
        else var_Z = (7.787 * var_Z) + (16.0 / 116.0);

        double l = (116.0 * var_Y) - 16.0;
        double a = 500.0 * (var_X - var_Y);
        double b = 200.0 * (var_Y - var_Z);

        return new double[]{l, a, b};
    }
}

package com.example.flightbooking;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import com.example.flightbooking.models.Booking;
import com.example.flightbooking.models.Flight;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PdfGenerator {

    /**
     * Generates a PDF ticket for the given booking and saves it to the downloads folder.
     */
    public static void generateTicketPdf(Context context, Booking booking) {
        PdfDocument document = new PdfDocument();
        
        // Page specification (A4 size or similar)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        // 1. Draw Background/Header
        paint.setColor(Color.parseColor("#22C55E")); // Green primary
        canvas.drawRect(0, 0, 595, 200, paint);
        
        // 2. Title
        paint.setColor(Color.WHITE);
        paint.setTextSize(36);
        paint.setFakeBoldText(true);
        canvas.drawText("Ethioairline E-Ticket", 50, 80, paint);
        
        paint.setTextSize(18);
        paint.setFakeBoldText(false);
        canvas.drawText(booking.getTravelClass() + " Class", 50, 120, paint);
        
        // 3. Flight Codes
        paint.setTextSize(48);
        paint.setFakeBoldText(true);
        Flight f = booking.getFlight();
        String fromCode = f != null && f.getFromCode() != null ? f.getFromCode() : (booking.getFromCode() != null ? booking.getFromCode() : "N/A");
        String toCode = f != null && f.getToCode() != null ? f.getToCode() : (booking.getToCode() != null ? booking.getToCode() : "N/A");
        canvas.drawText(fromCode, 50, 300, paint);
        canvas.drawText(toCode, 400, 300, paint);
        
        // 4. Details
        paint.setColor(Color.BLACK);
        paint.setTextSize(14);
        paint.setFakeBoldText(false);
        
        int startY = 400;
        int spacing = 40;
        
        String flightNumber = f != null && f.getFlightNumber() != null ? f.getFlightNumber() : "N/A";
        String fromTime = f != null && f.getFromTime() != null ? f.getFromTime() : (booking.getFromTime() != null ? booking.getFromTime() : "N/A");
        String term = f != null && f.getTerminalFrom() != null ? f.getTerminalFrom() : "N/A";
        
        drawLabelValue(canvas, paint, "PASSENGER", booking.getPassengerName() != null ? booking.getPassengerName() : "N/A", 50, startY);
        drawLabelValue(canvas, paint, "FLIGHT DATE", booking.getFlightDate() != null ? booking.getFlightDate() : (booking.getDate() != null ? booking.getDate() : "N/A"), 300, startY);
        
        startY += spacing * 2;
        drawLabelValue(canvas, paint, "FLIGHT #", flightNumber, 50, startY);
        drawLabelValue(canvas, paint, "GATE / SEAT", (booking.getGate() != null ? booking.getGate() : "TBD") + " / " + (booking.getSeat() != null ? booking.getSeat() : "TBD"), 300, startY);
        
        startY += spacing * 2;
        drawLabelValue(canvas, paint, "DEPARTURE", fromTime, 50, startY);
        drawLabelValue(canvas, paint, "PNR CODE", booking.getPnrCode() != null ? booking.getPnrCode() : "N/A", 300, startY);

        startY += spacing * 2;
        drawLabelValue(canvas, paint, "TERMINAL", term, 50, startY);
        drawLabelValue(canvas, paint, "TOTAL PAID", booking.getTotalAmount() != null ? booking.getTotalAmount() : (booking.getPrice() != null ? booking.getPrice() : "N/A"), 300, startY);
        
        // 5. Border
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.LTGRAY);
        canvas.drawRect(30, 220, 565, 750, paint);
        
        document.finishPage(page);
        
        // Save to public Documents or Downloads folder for easy access
        String pnr = booking.getPnrCode() != null ? booking.getPnrCode() : String.valueOf(System.currentTimeMillis());
        String fileName = "Ticket_" + pnr + ".pdf";
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EthioAirlines");
                
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                        if (out != null) {
                            document.writeTo(out);
                            Toast.makeText(context, "Ticket saved to Downloads/EthioAirlines", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } else {
                File publicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EthioAirlines");
                if (!publicDir.exists()) publicDir.mkdirs();
                
                File file = new File(publicDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    document.writeTo(fos);
                    Toast.makeText(context, "Ticket saved to Downloads: " + file.getName(), Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }
    
    private static void drawLabelValue(Canvas canvas, Paint paint, String label, String value, int x, int y) {
        paint.setColor(Color.GRAY);
        paint.setTextSize(10);
        paint.setFakeBoldText(true);
        canvas.drawText(label, x, y, paint);
        
        paint.setColor(Color.BLACK);
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText(value, x, y + 25, paint);
    }
}

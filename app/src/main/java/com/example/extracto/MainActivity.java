package com.example.extracto;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private static final int STORAGE_PERMISSION_REQUEST = 2;
    private static final String[] requiredPermissionList = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
    };

    private Button btnUpload;
    private TableLayout tableLayout;
    private boolean headerAdded;

    private ArrayList<String> namesList;
    private ArrayList<String> prnsList;
    private ArrayList<String> courseNamesList;
    private ArrayList<String> gradePointsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUpload = findViewById(R.id.btnUpload);
        tableLayout = findViewById(R.id.tableLayout);
        headerAdded = false;

        namesList = new ArrayList<>();
        prnsList = new ArrayList<>();
        courseNamesList = new ArrayList<>();
        gradePointsList = new ArrayList<>();

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStoragePermission();
            }
        });
    }

    private void checkStoragePermission() {
        // Check if storage permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openFilePicker();
        } else {
            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST);
        }
    }

    @SuppressWarnings("deprecation")
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_PDF_REQUEST);
    }

    private void extractInformationFromPdf(Uri pdfUri) {
        namesList.clear();
        prnsList.clear();
        courseNamesList.clear();
        gradePointsList.clear();

        // Extract information from the PDF
        // ...

        // Save data to Excel
        saveDataToExcel();

        displayTable();
    }

    private void saveDataToExcel() {
        try {
            // Create a new Excel workbook
            Workbook workbook = new HSSFWorkbook();

            // Create a new sheet
            Sheet sheet = workbook.createSheet("Grades");

            // Write the header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("PRN");
            headerRow.createCell(2).setCellValue("Course Name");
            headerRow.createCell(3).setCellValue("Grade Point");

            // Write the data rows
            for (int i = 0; i < namesList.size(); i++) {
                Row dataRow = sheet.createRow(i + 1);
                dataRow.createCell(0).setCellValue(namesList.get(i));
                dataRow.createCell(1).setCellValue(prnsList.get(i));
                dataRow.createCell(2).setCellValue(courseNamesList.get(i));
                dataRow.createCell(3).setCellValue(gradePointsList.get(i));
            }

            // Auto-size columns
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            // Save the workbook to a file
            File outputFile = new File(Environment.getExternalStorageDirectory(), "Grades.xls");
            FileOutputStream fileOut = new FileOutputStream(outputFile);
            workbook.write(fileOut);
            fileOut.close();

            Toast.makeText(this, "Data saved to " + outputFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error occurred while saving data to Excel", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayTable() {
        tableLayout.removeAllViews();

        for (int i = -1; i < namesList.size(); i++) {
            TableRow row = new TableRow(this);

            String name = (i == -1) ? "Name" : namesList.get(i);
            String prn = (i == -1) ? "PRN" : prnsList.get(i);
            String course = (i == -1) ? "Course" : courseNamesList.get(i);
            String grade = (i == -1) ? "Grade" : gradePointsList.get(i);

            TextView tvName = new TextView(this);
            TextView tvPrn = new TextView(this);
            TextView tvCourse = new TextView(this);
            TextView tvGrade = new TextView(this);

            tvName.setText(name);
            tvPrn.setText(prn);
            tvCourse.setText(course);
            tvGrade.setText(grade);

            row.addView(tvName);
            row.addView(tvPrn);
            row.addView(tvCourse);
            row.addView(tvGrade);

            tableLayout.addView(row);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            boolean allPermissionsGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                openFilePicker();
            } else {
                ActivityCompat.requestPermissions(this, requiredPermissionList, STORAGE_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK) {
            Uri pdfUri = data.getData();
            extractInformationFromPdf(pdfUri);
        }
    }
}

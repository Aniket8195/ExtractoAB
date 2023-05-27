package com.example.extracto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private static final int STORAGE_PERMISSION_REQUEST = 2;
    private static final int SAVE_EXCEL_REQUEST = 3;

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

    private void extractInformationFromPdf(Uri pdfUri) throws IOException {
        namesList.clear();
        prnsList.clear();
        courseNamesList.clear();
        gradePointsList.clear();

        InputStream inputStream = getContentResolver().openInputStream(pdfUri);
        PdfReader reader = new PdfReader(inputStream);

        int numPages = reader.getNumberOfPages();
        boolean courseSectionStarted = false;

        for (int i = 1; i <= numPages; i++) {
            extractDataFromPage(reader, i, courseSectionStarted);
            courseSectionStarted = true;
        }


        displayTable();
        saveDataToExcel();
        writeDataToExcel(pdfUri);
    }
    private void extractDataFromPage(PdfReader reader, int pageNum, boolean courseSectionStarted) {
        try {
            // Extract text from the specified page
            String pageText = PdfTextExtractor.getTextFromPage(reader, pageNum);

            // Process the extracted text
            String[] lines = pageText.split("\n");

            boolean pageHeaderFound = false; // Track if the page header is already found
            boolean namePrnAdded = false; // Track if name and PRN are already added

            for (String line : lines) {
                // Check if the course section has started
                if (line.contains("Semester")) {
                    // Append the extracted data to the tableLayout
                    if (!pageHeaderFound) {
                        addTableHeader();
                        pageHeaderFound = true;
                    }

                    if (!namePrnAdded) {
                        addTableRow(extractName(lines), extractPRN(lines), "", ""); // Add the name and PRN to the table
                        namePrnAdded = true;
                    }

                    courseSectionStarted = true;
                }

                // Exclude specific sections
                if (line.contains("PIMPRI CHINCHWAD EDUCATION TRUST's") ||
                        line.contains("PIMPRI CHINCHWAD COLLEGE OF ENGINEERING,") ||
                        line.contains("Statement of Grades") ||
                        line.contains("Semester III Semester IV Cumulative Semester Record")) {
                    courseSectionStarted = false;
                }

                // Extract course name and grade points
                if (courseSectionStarted) {
                    String[] courseData = extractCourseData(line);
                    if (courseData != null && courseData.length == 2) {
                        addTableRow("", "", courseData[0], courseData[1]); // Add the course name and grade points to the table

                        // Add the extracted data to the respective ArrayLists
                        courseNamesList.add(courseData[0]);
                        gradePointsList.add(courseData[1]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error occurred while extracting data from page " + pageNum, Toast.LENGTH_SHORT).show();
        }
    }
    private void addTableHeader() {
        TableRow headerRow = new TableRow(this);

        TextView nameHeader = new TextView(this);
        nameHeader.setText("Name");
        nameHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(nameHeader);

        TextView prnHeader = new TextView(this);
        prnHeader.setText("PRN");
        prnHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(prnHeader);

        TextView courseNameHeader = new TextView(this);
        courseNameHeader.setText("Course Name");
        courseNameHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(courseNameHeader);

        TextView gradePointsHeader = new TextView(this);
        gradePointsHeader.setText("Grade Points");
        gradePointsHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(gradePointsHeader);

        tableLayout.addView(headerRow);
    }

    private void addTableRow(String name, String prn, String courseName, String gradePoints) {
        TableRow tableRow = new TableRow(this);

        TextView nameTextView = new TextView(this);
        nameTextView.setText(name);
        tableRow.addView(nameTextView);

        TextView prnTextView = new TextView(this);
        prnTextView.setText(prn);
        tableRow.addView(prnTextView);

        TextView courseNameTextView = new TextView(this);
        courseNameTextView.setText(courseName);
        tableRow.addView(courseNameTextView);

        TextView gradePointsTextView = new TextView(this);
        gradePointsTextView.setText(gradePoints);
        tableRow.addView(gradePointsTextView);

        tableLayout.addView(tableRow);
    }

    private String extractName(String[] lines) {
        for (String line : lines) {
            // Extract the name (only the first occurrence)
            if (line.contains("Name :")) {
                return line.substring(line.indexOf("Name :") + 7).trim();
            }
        }
        return "";
    }

    private String extractPRN(String[] lines) {
        for (String line : lines) {
            // Extract the PRN
            if (line.contains("PRN :")) {
                return line.substring(line.indexOf("PRN :") + 6).trim();
            }
        }
        return "";
    }

    private String[] extractCourseData(String line) {
        String[] courseData = line.trim().split("\\s+");
        if (courseData.length >= 6) {
            StringBuilder courseNameBuilder = new StringBuilder();
            for (int i = 2; i < courseData.length - 2; i++) {
                courseNameBuilder.append(courseData[i]).append(" ");
            }
            String courseName = courseNameBuilder.toString().trim();
            String gradePoints = courseData[courseData.length - 1];
            return new String[]{courseName, gradePoints};
        }
        return null;
    }
    private void saveDataToExcel() {

//      //  File outputFile = new File(getExternalFilesDir(null), + "/Grades.xls");
//        File outputFile=new File(Environment.getExternalStorageDirectory()+"/Grades.xls");
//
//        try {
//            // Create a new Excel workbook
//            Workbook workbook = new HSSFWorkbook();
//
//            // Create a new sheet
//            Sheet sheet = workbook.createSheet("Grades");
//
//            // Write the header row
//           Row headerRow = sheet.createRow(0);
//            headerRow.createCell(0).setCellValue("Name");
//            headerRow.createCell(1).setCellValue("PRN");
//            headerRow.createCell(2).setCellValue("Course Name");
//            headerRow.createCell(3).setCellValue("Grade Point");
//
//            // Write the data rows
//            for (int i = 0; i < namesList.size(); i++) {
//                Row dataRow = sheet.createRow(i + 1);
//               dataRow.createCell(0).setCellValue(namesList.get(i));
//                dataRow.createCell(1).setCellValue(prnsList.get(i));
//                dataRow.createCell(2).setCellValue(courseNamesList.get(i));
//                dataRow.createCell(3).setCellValue(gradePointsList.get(i));
//            }
//
//            // Auto-size columns
//            for (int i = 0; i < 4; i++) {
//                sheet.autoSizeColumn(i);
//            }
//
//            // Save the workbook to a file
//
//            FileOutputStream fileOut = new FileOutputStream(outputFile);
//            workbook.write(fileOut);
//            fileOut.close();
//
//           Toast.makeText(this, "Data saved to " + outputFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(this, "Error occurred while saving data to Excel", Toast.LENGTH_SHORT).show();
//        }
////        HSSFWorkbook
////        try {
////            if(outputFile.exists()){
////
////                outputFile.createNewFile();
////
////
////            }
////        }catch (Exception e){
////            e.printStackTrace();
////        }

//        String directoryPath = getExternalFilesDir(null).getAbsolutePath() + "/MyExcelFiles";
//        File directory = new File(directoryPath);
//        directory.mkdirs(); // Create the directory if it doesn't exist
//
//        String filePath = directoryPath + "/output.xlsx";
//
//
//        // Create a new workbook
//        try (Workbook workbook = new XSSFWorkbook()) {
//            // Create a new sheet
//            Sheet sheet = workbook.createSheet("Sheet1");
//
//            // Write data to the sheet
//            for (int i = 0; i < 10; i++) {
//                Row row = sheet.createRow(i);
//                for (int j = 0; j < 5; j++) {
//                    Cell cell = row.createCell(j);
//                    // Specify the cell value
//                    cell.setCellValue("Value " + i + "-" + j);
//                }
//            }
//
//            // Write the workbook to the output file
//            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
//                workbook.write(fileOut);
//            }
//
//            System.out.println("Excel file created successfully.");
//            Toast.makeText(this, "Excel file created successfully.", Toast.LENGTH_SHORT).show();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        String fileName = "output.xlsx";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
      //  startActivityForResult(intent, SAVE_EXCEL_REQUEST);
        createDocumentLauncher.launch(fileName);
    }
    private final ActivityResultLauncher<String> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(),
            result -> {
                if (result != null) {
                    try {
                        writeDataToExcel(result);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, "Error writing to Excel file", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Error creating the Excel file", Toast.LENGTH_SHORT).show();
                }
            }
    );
    private void writeDataToExcel(Uri excelUri) throws IOException {
        // Open the output stream for writing the data to the selected Excel file
        OutputStream outputStream = getContentResolver().openOutputStream(excelUri);
        if (outputStream == null) {
            Toast.makeText(this, "Error opening the Excel file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new workbook
        Workbook workbook = new XSSFWorkbook();

        // Create a new sheet
        Sheet sheet = workbook.createSheet("Grades");

        // Write the header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("PRN");
        headerRow.createCell(2).setCellValue("Course Name");
        headerRow.createCell(3).setCellValue("Grade Points");

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

        // Write the workbook to the output stream
        workbook.write(outputStream);

        // Close the workbook and output stream
        workbook.close();
        outputStream.close();

        Toast.makeText(this, "Data saved to Excel file", Toast.LENGTH_SHORT).show();
    }

    private void displayTable() {
        tableLayout.removeAllViews();
        Log.e("h","h");
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
            try {
                extractInformationFromPdf(pdfUri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (requestCode == SAVE_EXCEL_REQUEST && resultCode == RESULT_OK) {
            Uri excelUri = data.getData();
            // TODO: Save the Excel file using the excelUri
        }
    }

}
package org.sang.labmanagement.asset.importexport;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import jakarta.transaction.Transactional;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.sang.labmanagement.asset.Asset;
import org.sang.labmanagement.asset.AssetRepository;
import org.sang.labmanagement.asset.AssetStatus;
import org.sang.labmanagement.asset.category.Category;
import org.sang.labmanagement.asset.category.CategoryRepository;
import org.sang.labmanagement.asset.location.Location;
import org.sang.labmanagement.asset.location.LocationRepository;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetImportExportService {

	private final AssetRepository assetRepository;
	private final LocationRepository locationRepository;
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;

	// Định dạng ngày giờ
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	// Import từ CSV
	@Transactional
	public void importAssetsFromCSV(MultipartFile file) throws IOException, CsvException {
		List<Asset> assets = new ArrayList<>();
		try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
			List<String[]> records = csvReader.readAll();
			// Bỏ qua tiêu đề và dòng tiêu đề tùy chỉnh nếu có
			for (int i = 1; i < records.size(); i++) {
				String[] record = records.get(i);
				// Đảm bảo rằng record có đủ cột
				if (record.length < 9) { // Nếu đã thêm các cột mới
					continue; // hoặc xử lý lỗi
				}
				String locationName=record[6];
				Location location=locationRepository.findByName(locationName).orElseThrow(
						()->new  ResourceNotFoundException("Location Name not found")
				);
				String categoryName=record[7];
				Category category=categoryRepository.findByName(categoryName).orElseThrow(
						()->new  ResourceNotFoundException("Category Name not found")
				);
				String assignedUserName=record[8];
				User user=userRepository.findByUsername(categoryName).orElseThrow(
						()->new  ResourceNotFoundException("Username not found")
				);

				Asset asset = Asset.builder()
						.name(record[0])
						.description(record[1])
						.serialNumber(record[2])
						.status(AssetStatus.valueOf(record[3]))
						.purchaseDate(LocalDateTime.parse(record[4], DATE_TIME_FORMATTER))
						.price(Double.parseDouble(record[5]))
						.location(location)
						.category(category)
						.assignedUser(user)
						.build();
				assets.add(asset);
			}
		}
		assetRepository.saveAll(assets);
	}

	// Export sang CSV
	public ByteArrayInputStream exportAssetsToCSV() throws IOException {
		String[] HEADERs = { "Name", "Description", "Serial Number", "Status", "Purchase Date", "Price", "Location Name", "Category Name", "Assigned User Name" };
		List<Asset> assets = assetRepository.findAll();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(out);

		// Viết tiêu đề
		writer.println("International University Asset Management");
		// Viết một dòng trống để tạo khoảng cách
		writer.println();

		// Viết header
		writer.println(String.join(",", HEADERs));

		// Viết dữ liệu
		for (Asset asset : assets) {
			String locationName = (asset.getLocation() != null) ? asset.getLocation().getName() : "N/A";
			String categoryName = (asset.getCategory() != null) ? asset.getCategory().getName() : "N/A";
			String assignedUserName = (asset.getAssignedUser() != null) ? asset.getAssignedUser().getName() : "N/A";

			String[] data = {
					escapeSpecialCharacters(asset.getName()),
					escapeSpecialCharacters(asset.getDescription()),
					escapeSpecialCharacters(asset.getSerialNumber()),
					asset.getStatus().name(),
					asset.getPurchaseDate().format(DATE_TIME_FORMATTER),
					asset.getPrice().toString(),
					escapeSpecialCharacters(locationName),
					escapeSpecialCharacters(categoryName),
					escapeSpecialCharacters(assignedUserName)
			};
			writer.println(String.join(",", data));
		}

		writer.flush();
		return new ByteArrayInputStream(out.toByteArray());
	}

	// Helper method to handle commas and quotes in CSV
	private String escapeSpecialCharacters(String data) {
		if (data == null) {
			return "";
		}
		String escapedData = data.replaceAll("\"", "\"\"");
		if (escapedData.contains(",") || escapedData.contains("\"") || escapedData.contains("\n")) {
			escapedData = "\"" + escapedData + "\"";
		}
		return escapedData;
	}

	// Import từ Excel
	@Transactional
	public void importAssetsFromExcel(MultipartFile file) throws IOException {
		List<Asset> assets = new ArrayList<>();
		try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rows = sheet.iterator();

			// Bỏ qua dòng tiêu đề và dòng header
			if (rows.hasNext()) rows.next(); // Tiêu đề
			if (rows.hasNext()) rows.next(); // Header

			while (rows.hasNext()) {
				Row currentRow = rows.next();
				String locationName= String.valueOf(currentRow.getCell(6));
				Location location=locationRepository.findByName(locationName).orElseThrow(
						()->new  ResourceNotFoundException("Location Name not found")
				);
				String categoryName= String.valueOf(currentRow.getCell(7));
				Category category=categoryRepository.findByName(categoryName).orElseThrow(
						()->new  ResourceNotFoundException("Category Name not found")
				);
				String assignedUserName= String.valueOf(currentRow.getCell(8));
				User user=userRepository.findByUsername(categoryName).orElseThrow(
						()->new  ResourceNotFoundException("Username not found")
				);
				Asset asset = Asset.builder()
						.name(getCellValueAsString(currentRow.getCell(0)))
						.description(getCellValueAsString(currentRow.getCell(1)))
						.serialNumber(getCellValueAsString(currentRow.getCell(2)))
						.status(AssetStatus.valueOf(getCellValueAsString(currentRow.getCell(3))))
						.purchaseDate(LocalDateTime.parse(getCellValueAsString(currentRow.getCell(4)), DATE_TIME_FORMATTER))
						.location(location)
						.category(category)
						.assignedUser(user)
						.build();
				assets.add(asset);
			}
		}
		assetRepository.saveAll(assets);
	}

	// Helper method to safely get cell values as String
	private String getCellValueAsString(Cell cell) {
		if (cell == null) {
			return "";
		}
		DataFormatter formatter = new DataFormatter();
		return formatter.formatCellValue(cell);
	}

	// Export sang Excel
	public ByteArrayInputStream exportAssetsToExcel() throws IOException {
		String[] HEADERs = { "Name", "Description", "Serial Number", "Status", "Purchase Date", "Price", "Location Name", "Category Name", "Assigned User Name" };
		List<Asset> assets = assetRepository.findAll();

		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Assets");

		int rowIdx = 0;

		// Tạo dòng tiêu đề
		Row titleRow = sheet.createRow(rowIdx++);
		Cell titleCell = titleRow.createCell(0);
		titleCell.setCellValue("International University Asset Management");

		// Gộp ô cho tiêu đề
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERs.length - 1));

		// Tạo kiểu cho tiêu đề
		CellStyle titleStyle = workbook.createCellStyle();
		Font titleFont = workbook.createFont();
		titleFont.setFontHeightInPoints((short) 16);
		titleFont.setBold(true);
		titleStyle.setFont(titleFont);
		titleStyle.setAlignment(HorizontalAlignment.CENTER);
		titleCell.setCellStyle(titleStyle);

		// Tạo dòng header
		Row headerRow = sheet.createRow(rowIdx++);
		CellStyle headerStyle = workbook.createCellStyle();
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle.setAlignment(HorizontalAlignment.CENTER);
		headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		headerStyle.setBorderBottom(BorderStyle.THIN);
		headerStyle.setBorderTop(BorderStyle.THIN);
		headerStyle.setBorderLeft(BorderStyle.THIN);
		headerStyle.setBorderRight(BorderStyle.THIN);

		for (int col = 0; col < HEADERs.length; col++) {
			Cell cell = headerRow.createCell(col);
			cell.setCellValue(HEADERs[col]);
			cell.setCellStyle(headerStyle);
		}

		// Tạo kiểu cho dữ liệu
		CellStyle dataStyle = workbook.createCellStyle();
		dataStyle.setAlignment(HorizontalAlignment.LEFT);
		dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		dataStyle.setWrapText(true);
		dataStyle.setBorderBottom(BorderStyle.THIN);
		dataStyle.setBorderTop(BorderStyle.THIN);
		dataStyle.setBorderLeft(BorderStyle.THIN);
		dataStyle.setBorderRight(BorderStyle.THIN);

		// Tạo dòng dữ liệu
		for (Asset asset : assets) {
			Row row = sheet.createRow(rowIdx++);

			String locationName = (asset.getLocation() != null) ? asset.getLocation().getName() : "N/A";
			String categoryName = (asset.getCategory() != null) ? asset.getCategory().getName() : "N/A";
			String assignedUserName = (asset.getAssignedUser() != null) ? asset.getAssignedUser().getName() : "N/A";

			String[] data = {
					asset.getName(),
					asset.getDescription(),
					asset.getSerialNumber(),
					asset.getStatus().name(),
					asset.getPurchaseDate().format(DATE_TIME_FORMATTER),
					asset.getPrice().toString(),
					locationName,
					categoryName,
					assignedUserName
			};

			for (int col = 0; col < data.length; col++) {
				Cell cell = row.createCell(col);
				cell.setCellValue(data[col]);
				cell.setCellStyle(dataStyle);
			}
		}

		// Tự động điều chỉnh kích thước cột
		for (int col = 0; col < HEADERs.length; col++) {
			sheet.autoSizeColumn(col);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		workbook.write(out);
		workbook.close();

		return new ByteArrayInputStream(out.toByteArray());
	}
}

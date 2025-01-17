package org.sang.labmanagement.asset.category;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class CategoryController {
	private final CategoryService categoryService;

	@GetMapping
	public ResponseEntity<PageResponse<Category>>getAllCategories(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	){
		return ResponseEntity.ok(categoryService.getAllCategories(page,size));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
		Category category = categoryService.getCategoryById(id);
		return ResponseEntity.ok(category);
	}

	@PostMapping
	public ResponseEntity<Category> createCategory(@RequestBody Category category) {
		Category createdCategory = categoryService.createCategory(category);
		return ResponseEntity.ok(createdCategory);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails) {
		Category updatedCategory = categoryService.updateCategory(id, categoryDetails);
		return ResponseEntity.ok(updatedCategory);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
		categoryService.deleteCategory(id);
		return ResponseEntity.ok("Category deleted successfully!");
	}
}

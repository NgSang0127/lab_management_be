package org.sang.labmanagement.asset.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {
	private final CategoryService categoryService;

	@GetMapping
	public ResponseEntity<PageResponse<Category>> getAllCategories(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size
	) {
		log.info("Fetching categories with params: page={}, size={}", page, size);
		PageResponse<Category> response = getCachedCategories(page, size);
		log.info("Returning {} categories", response.getContent().size());
		return ResponseEntity.ok(response);
	}

	@Cacheable(value = "categories", key = "#page + '-' + #size")
	public PageResponse<Category> getCachedCategories(int page, int size) {
		return categoryService.getAllCategories(page, size);
	}

	@GetMapping("/{id}")
	@Cacheable(value = "category", key = "#id")
	public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
		log.info("Fetching category with id: {}", id);
		Category category = categoryService.getCategoryById(id);
		log.info("Returning category: {}", category.getId());
		return ResponseEntity.ok(category);
	}

	@PostMapping
	@CachePut(value = "category", key = "#result.id")
	@CacheEvict(value = "categories", allEntries = true)
	public ResponseEntity<Category> createCategory(@RequestBody Category category) {
		log.info("Creating category: {}", category);
		Category createdCategory = categoryService.createCategory(category);
		log.info("Created category with id: {}", createdCategory.getId());
		return ResponseEntity.ok(createdCategory);
	}

	@PutMapping("/{id}")
	@CachePut(value = "category", key = "#id")
	@CacheEvict(value = "categories", allEntries = true)
	public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails) {
		log.info("Updating category with id: {}", id);
		Category updatedCategory = categoryService.updateCategory(id, categoryDetails);
		log.info("Updated category: {}", updatedCategory.getId());
		return ResponseEntity.ok(updatedCategory);
	}

	@DeleteMapping("/{id}")
	@CacheEvict(value = {"category", "categories"}, key = "#id")
	public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
		log.info("Deleting category with id: {}", id);
		categoryService.deleteCategory(id);
		log.info("Deleted category with id: {}", id);
		return ResponseEntity.ok("Category deleted successfully!");
	}
}
package org.sang.labmanagement.asset.category;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceAlreadyExistsException;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;

	@Cacheable(value = "categories",key ="#page + '-' + #size")
	public PageResponse<Category> getAllCategories(int page,int size){
		Pageable pageable= PageRequest.of(page,size);
		Page<Category> categories=categoryRepository.findAll(pageable);
		return PageResponse.<Category>builder()
				.content(categories.getContent())
				.number(categories.getNumber())
				.size(categories.getSize())
				.totalElements(categories.getTotalElements())
				.totalPages(categories.getTotalPages())
				.first(categories.isFirst())
				.last(categories.isLast())
				.build();
	}

	@CacheEvict(value = "categories", allEntries = true)
	public Category createCategory(Category category){
		if(categoryRepository.existsByName(category.getName())){
			throw new ResourceAlreadyExistsException("Category already exists with name :"+category.getName());
		};
		return categoryRepository.save(category);
	}

	@Cacheable(value = "category" ,key="#id")
	public Category getCategoryById(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
	}

	@CacheEvict(value = {"category", "categories"}, key = "#id", allEntries = true)
	public Category updateCategory(Long id, Category categoryDetails) {
		Category category = getCategoryById(id);
		category.setName(categoryDetails.getName());
		category.setDescription(categoryDetails.getDescription());
		return categoryRepository.save(category);
	}

	@CacheEvict(value = {"category", "categories"}, key = "#id", allEntries = true)
	public void deleteCategory(Long id) {
		Category category = getCategoryById(id);
		categoryRepository.delete(category);
	}
}

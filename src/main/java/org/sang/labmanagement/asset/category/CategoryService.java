package org.sang.labmanagement.asset.category;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;

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

	public Category createCategory(Category category){
		if(categoryRepository.existsByName(category.getName())){
			throw new RuntimeException("Category already exists with name :"+category.getName());
		};
		return categoryRepository.save(category);
	}

	public Category getCategoryById(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
	}

	public Category updateCategory(Long id, Category categoryDetails) {
		Category category = getCategoryById(id);
		category.setName(categoryDetails.getName());
		category.setDescription(categoryDetails.getDescription());
		return categoryRepository.save(category);
	}

	public void deleteCategory(Long id) {
		Category category = getCategoryById(id);
		categoryRepository.delete(category);
	}
}

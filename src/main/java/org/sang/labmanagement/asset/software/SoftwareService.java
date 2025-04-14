package org.sang.labmanagement.asset.software;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceAlreadyExistsException;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SoftwareService {
	private final SoftwareRepository softwareRepository;

	public PageResponse<Software> getAllSoftwares(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Software> softwares = softwareRepository.findAll(pageable);
		PageResponse<Software> response = new PageResponse<>();
		response.setContent(softwares.getContent());
		response.setNumber(softwares.getNumber());
		response.setSize(softwares.getSize());
		response.setTotalElements(softwares.getTotalElements());
		response.setTotalPages(softwares.getTotalPages());
		response.setLast(softwares.isLast());
		return response;
	}

	public Software createSoftware(Software software){
		if(softwareRepository.existsBySoftwareName(software.getSoftwareName())){
			throw new ResourceAlreadyExistsException("Software name already exists with name: "+software.getSoftwareName());
		}

		return softwareRepository.save(software);
	}

	public Software getSoftwareById(Long id){
		return softwareRepository.findById(id)
				.orElseThrow(()-> new ResourceNotFoundException("Software not found with id :"+id));
	}

	public Software updateSoftware(Long id,Software softwareDetails){
		Software software=getSoftwareById(id);
		software.setSoftwareName(softwareDetails.getSoftwareName());
		software.setIsFree(softwareDetails.getIsFree());
		return softwareRepository.save(software);
	}

	public void deleteSoftware(Long id){
		Software software=getSoftwareById(id);
		softwareRepository.delete(software);
	}

	public List<Software> getSoftwaresByRoomId(Long roomId) {
		return softwareRepository.findByRoomId(roomId);
	}
}

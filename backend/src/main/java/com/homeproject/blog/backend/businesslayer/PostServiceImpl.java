package com.homeproject.blog.backend.businesslayer;

import com.homeproject.blog.backend.data.entity.PostEntity;
import com.homeproject.blog.backend.data.entity.TagEntity;
import com.homeproject.blog.backend.data.entity.converters.PostConverter;
import com.homeproject.blog.backend.data.repository.PostRepository;
import com.homeproject.blog.backend.dtos.Post;
import com.homeproject.blog.backend.exceptions.PostNotFoundException;
import com.homeproject.blog.backend.supportclasses.AppStartupRunner;
import com.homeproject.blog.backend.supportclasses.CurrentDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private TagService tagService;
    @Autowired
    private PostConverter postConverter;


    @Override
    public Post createPost(Post post) {
        String date = CurrentDate.getDate();
        List<TagEntity> tagEntities = tagService.identifyTags(post.getTags());
        PostEntity newPost = new PostEntity();
        newPost.setTags(tagEntities);
        newPost.setTitle(post.getTitle());
        newPost.setText(post.getText());
        newPost.setAuthor(AppStartupRunner.userEntity);
        newPost.setPreviewAttachment(post.getPreviewAttachment());
        newPost.setUpdatedOn(date);
        newPost.setCreatedOn(date);
        PostEntity savedPost = postRepository.save(newPost);
        return postConverter.entityToPost(savedPost);
    }

    @Override
    public Post updatePost(Long id, Post changes) throws PostNotFoundException {
        List<TagEntity> tagEntities = tagService.identifyTags(changes.getTags());
        PostEntity entity = verifyPostExisting(id);
        entity.setText(changes.getText());
        entity.setTags(tagEntities);
        entity.setTitle(changes.getTitle());
        entity.setPreviewAttachment(changes.getPreviewAttachment());
        entity.setUpdatedOn(CurrentDate.getDate());
        PostEntity updatedEntity = postRepository.save(entity);
        return postConverter.entityToPost(updatedEntity);
    }

    private PostEntity verifyPostExisting(Long id) throws PostNotFoundException {
        Optional<PostEntity> result = postRepository.findById(id);
        if (result.isEmpty()) {
            throw new PostNotFoundException();
        }
        return result.get();
    }

    @Override
    public Post readPost(Long id) throws PostNotFoundException {
        PostEntity entity = verifyPostExisting(id);
        return postConverter.entityToPost(entity);
    }

    @Override
    public Page<Post> getPosts(Map<String, String> parameters) {
        String sort = parameters.get("sort");
        Sort sorting;
        if (sort != null) {
            if (sort.charAt(0) == '-') {
                sorting = Sort.by(sort.substring(1)).descending();
            } else {
                sorting = Sort.by(sort);
            }
        } else {
            sorting = Sort.by("id").descending();
        }
        if (!parameters.containsKey("page_num")) {
            parameters.put("page_num", "0");
        }
        if (!parameters.containsKey("page_size")) {
            parameters.put("page_size", "10");
        }
        Integer pageNum = Integer.parseInt(parameters.get("page_num"));
        Integer pageSize = Integer.parseInt(parameters.get("page_size"));
        String idParam = parameters.get("id");
        Long id;
        if (idParam == null) {
            id = null;
        } else {
            id = Long.parseLong(idParam);
        }
        String tagIdParam = parameters.get("tag_id");
        Long tag_id;
        if (tagIdParam == null) {
            tag_id = null;
        } else {
            tag_id = Long.parseLong(tagIdParam);
        }
        PageRequest request = PageRequest.of(pageNum, pageSize, sorting);
        Page<PostEntity> entities = postRepository.findPostsByParameters(request, id, tag_id, parameters.get("tag_name"), parameters.get("author_name"));
        return new PageImpl<>(entities.stream().map(postConverter::entityToPost).collect(Collectors.toList()), request, entities.getTotalElements());
    }

    @Override
    public void deletePost(Long id) throws PostNotFoundException {
        verifyPostExisting(id);
        postRepository.deleteById(id);
    }
}

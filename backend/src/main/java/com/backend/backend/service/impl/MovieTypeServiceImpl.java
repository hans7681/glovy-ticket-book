package com.backend.backend.service.impl;

import com.backend.backend.entity.MovieType;
import com.backend.backend.mapper.MovieTypeMapper;
import com.backend.backend.service.MovieTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class MovieTypeServiceImpl extends ServiceImpl<MovieTypeMapper, MovieType> implements MovieTypeService {
    // ServiceImpl 已经实现了 IService 的大部分方法
    // 可以在这里覆盖或添加自定义的业务逻辑
} 
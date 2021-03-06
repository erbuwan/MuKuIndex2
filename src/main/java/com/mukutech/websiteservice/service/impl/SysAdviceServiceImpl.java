package com.mukutech.websiteservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mukutech.websiteservice.common.utils.BeanCopyUtil;
import com.mukutech.websiteservice.common.utils.ResultVOUtil;
import com.mukutech.websiteservice.common.utils.response.ResponseEnvelope;
import com.mukutech.websiteservice.pojo.entity.SysAdvice;
import com.mukutech.websiteservice.mapper.SysAdviceMapper;
import com.mukutech.websiteservice.pojo.dto.SysAdviceDTO;
import com.mukutech.websiteservice.service.ISysAdviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author TCGUO
 * @since 2020-07-23
 */
@Slf4j
@Transactional
@Service
public class SysAdviceServiceImpl extends ServiceImpl<SysAdviceMapper, SysAdvice> implements ISysAdviceService {


    @Autowired
    private SysAdviceMapper sysAdviceMapper;

    @Override
    public ResponseEnvelope searchSysAdviceListPage(SysAdviceDTO dto) {
        Page<SysAdvice> page = new Page<SysAdvice>();
        page.setSize(dto.getPageSize());
        page.setCurrent(dto.getCurrentPage());
        QueryWrapper<SysAdvice> queryWrapper = new QueryWrapper<SysAdvice>();
        SysAdvice entity = new SysAdvice();
        BeanCopyUtil.copyPropertiesIgnoreNull(dto, entity);
        queryWrapper.setEntity(entity);
        return ResultVOUtil.returnSuccess(sysAdviceMapper.selectPage(page, queryWrapper));
    }

    @Override
    public ResponseEnvelope searchSysAdviceOne(Long id) {

        return ResultVOUtil.returnSuccess(this.selectOne(id));
    }

    @Override
    public ResponseEnvelope addSysAdvice(SysAdviceDTO dto) {
        SysAdvice entity = new SysAdvice();
        BeanCopyUtil.copyPropertiesIgnoreNull(dto, entity);
        sysAdviceMapper.insert(entity);
        return ResultVOUtil.returnSuccess();
    }

    @Override
    public ResponseEnvelope updateSysAdvice(SysAdviceDTO dto) {
        SysAdvice entity = this.selectOne(dto.getId());
        BeanCopyUtil.copyPropertiesIgnoreNull(dto, entity);
        sysAdviceMapper.updateById(entity);
        return ResultVOUtil.returnSuccess();
    }

    @Override
    public ResponseEnvelope deleteSysAdvice(Long id) {
        sysAdviceMapper.deleteById(id);
        return ResultVOUtil.returnSuccess();
    }


    public SysAdvice selectOne(Long id) {
        SysAdvice entity = new SysAdvice();
        entity.setId(id);
        QueryWrapper<SysAdvice> queryWrapper = new QueryWrapper<>();
        queryWrapper.setEntity(entity);
        return sysAdviceMapper.selectOne(queryWrapper);
    }

}

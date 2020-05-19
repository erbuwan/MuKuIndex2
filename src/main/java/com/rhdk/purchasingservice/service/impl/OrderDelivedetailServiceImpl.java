package com.rhdk.purchasingservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rhdk.purchasingservice.common.utils.BeanCopyUtil;
import com.rhdk.purchasingservice.common.utils.ResultVOUtil;
import com.rhdk.purchasingservice.common.utils.TokenUtil;
import com.rhdk.purchasingservice.common.utils.response.ResponseEnvelope;
import com.rhdk.purchasingservice.feign.AssetServiceFeign;
import com.rhdk.purchasingservice.mapper.OrderDelivedetailMapper;
import com.rhdk.purchasingservice.pojo.dto.OrderDelivedetailDTO;
import com.rhdk.purchasingservice.pojo.entity.OrderDelivedetail;
import com.rhdk.purchasingservice.pojo.entity.OrderDelivemiddle;
import com.rhdk.purchasingservice.pojo.query.AssetQuery;
import com.rhdk.purchasingservice.pojo.query.OrderDelivedetailQuery;
import com.rhdk.purchasingservice.pojo.vo.OrderDelivedetailVO;
import com.rhdk.purchasingservice.pojo.vo.OrderDelivemiddleVO;
import com.rhdk.purchasingservice.service.IOrderDelivedetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 送货明细 服务实现类
 * </p>
 *
 * @author LMYOU
 * @since 2020-05-12
 */
@Slf4j
@Transactional
@Service
public class OrderDelivedetailServiceImpl extends ServiceImpl<OrderDelivedetailMapper, OrderDelivedetail> implements IOrderDelivedetailService {


    @Autowired
    private OrderDelivedetailMapper orderDelivedetailMapper;

    @Autowired
    private AssetServiceFeign assetServiceFeign;

    @Override
    public ResponseEnvelope searchOrderDelivedetailListPage(OrderDelivedetailQuery dto) {
        Page<OrderDelivedetail> page = new Page<OrderDelivedetail>();
        page.setSize(dto.getPageSize());
        page.setCurrent(dto.getCurrentPage());
        QueryWrapper<OrderDelivedetail> queryWrapper = new QueryWrapper<OrderDelivedetail>();
        OrderDelivedetail entity = new OrderDelivedetail();
        entity.setMiddleId(dto.getMiddleId());
        queryWrapper.setEntity(entity);
        page = orderDelivedetailMapper.selectPage(page, queryWrapper);
        List<OrderDelivedetail> resultList = page.getRecords();
        List<Long> assetIds = new ArrayList<>();
        List<OrderDelivedetailVO> orderDelivemiddleVOList = resultList.stream().map(a -> {
            assetIds.add(a.getAssetId());
            OrderDelivedetailVO model = OrderDelivedetailVO.builder().build();
            BeanCopyUtil.copyPropertiesIgnoreNull(a, model);
            return model;
        }).collect(Collectors.toList());
        //fegin调用资产服务，获取明细表格数据
        AssetQuery assetQuery=new AssetQuery();
        assetQuery.setAssetIds(assetIds);
        assetQuery.setAssetTemplId(dto.getModuleId());
        ResponseEnvelope result= assetServiceFeign.searchEntityInfoPage(assetQuery, TokenUtil.getToken());
        //HashMap<String,Object> titleMap=result.getData();
        Page<OrderDelivedetailVO> page2 = new Page<OrderDelivedetailVO>();
        page2.setRecords(orderDelivemiddleVOList);
        page2.setSize(page.getSize());
        page2.setCurrent(page.getCurrent());
        page2.setTotal(page.getTotal());
        page2.setOrders(page.getOrders());
        return ResultVOUtil.returnSuccess();
    }

    @Override
    public ResponseEnvelope searchOrderDelivedetailOne(Long id) {

        return ResultVOUtil.returnSuccess(this.selectOne(id));
    }

    @Override
    @Transactional
    public ResponseEnvelope addOrderDelivedetail(OrderDelivedetailDTO dto) {
        OrderDelivedetail entity = new OrderDelivedetail();
        BeanCopyUtil.copyPropertiesIgnoreNull(dto, entity);
        orderDelivedetailMapper.insert(entity);
        return ResultVOUtil.returnSuccess();
    }

    @Override
    public ResponseEnvelope updateOrderDelivedetail(OrderDelivedetailDTO dto) {
        OrderDelivedetail entity = this.selectOne(dto.getId());
        BeanCopyUtil.copyPropertiesIgnoreNull(dto, entity);
        orderDelivedetailMapper.updateById(entity);
        return ResultVOUtil.returnSuccess();
    }

    @Override
    public ResponseEnvelope deleteOrderDelivedetail(Long id) {
        orderDelivedetailMapper.deleteById(id);
        return ResultVOUtil.returnSuccess();
    }


    public OrderDelivedetail selectOne(Long id) {
        OrderDelivedetail entity = new OrderDelivedetail();
        entity.setId(id);
        QueryWrapper<OrderDelivedetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.setEntity(entity);
        return orderDelivedetailMapper.selectOne(queryWrapper);
    }

}

package com.dong.service;

import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 通过Redis实现签到、统计连续签到功能
 * 该方案无法获取用户具体的签到时间点
 */
@Service
public class SignInService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * sign:userId{666}:date{202308}
     */
    private static final String SIGN_USER_KEY = "sign:userId{%s}:date{%s}";

    /**
     * 用户签到
     */
    public void doSignIn(String userId) {
        //1.获取当前日期
        LocalDateTime now = LocalDateTime.now();
        //2.获取Redis key
        String key = String.format(SIGN_USER_KEY, userId, now.format(DateTimeFormatter.ofPattern("yyyyMM")));
        //3.今天是本月第几天
        int dayOfMonth = now.getDayOfMonth();
        //4.签到 位置从0开始的, 本月的第一条应该存在第0位, 因此需要减一
        //redis客户端查看时需要将格式改成binary格式
        redisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
    }

    /**
     * 连续签到统计
     * 例子中是当前月连续签到了多少天
     */
    public void continuousSignInStatistic(String userId) {
        //1.获取当前日期
        LocalDateTime now = LocalDateTime.now();
        //2.获取Redis key
        String key = String.format(SIGN_USER_KEY, userId, now.format(DateTimeFormatter.ofPattern("yyyyMM")));
        //3.今天是本月第几天
        int dayOfMonth = now.getDayOfMonth();
        //4.获取本月到今天为止的所有签到记录, 从0开始获取无符号位的, 返回的是一个十进制的数
        List<Long> result = redisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0));

        //没有签到记录
        if (result == null) {
            return;
        }

        //得到签到记录, 十进制表示
        Long num = result.get(0);
        //连续签到的天数
        int dayNum = 0;
        //循环遍历
        while (true) {
            //这个数字和1做与运算, 在二进制表示中，数字的每一位要么是 0，要么是 1。
            // 通过执行 num & 1 这个位运算，可以将 num 的二进制表示的最低位与二进制数 00000001（即只有最低位为 1，其余位都为 0）进行“与”操作。这将导致结果为 0 或 1，取决于 num 的最低位的值
            if ((num & 1) == 0) {
                // 如果是0就退出循环
                break;
            } else {
                //不为0说明已签到, 连续天数+1
                dayNum++;
            }
            //将 num 向右逻辑位移一位，即将二进制表示向右移动一位。这是为了在下一次迭代中检查下一个位
            //这边相当于从尾部开始循环直到碰到0为止
            num >>>= 1;
        }
        //最后得到的就是连续签到的天数
        System.out.println(dayNum);
    }

    @PostConstruct
    public void run() {
        //doSignIn("666");
        continuousSignInStatistic("666");
    }
}

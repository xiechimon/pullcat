package com.pullcat.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GitHub Webhook 事件请求 DTO
 */
@Data
public class WebhookEventReqDTO {

    /**
     * 事件动作
     */
    private String action;

    /**
     * Pull Request 数据
     */
    @JsonProperty("pull_request")
    private WebhookPullRequestReqDTO pullRequest;

    /**
     * 安装信息
     */
    private InstallationReqDTO installation;

    /**
     * 安装信息请求 DTO
     */
    @Data
    public static class InstallationReqDTO {

        /**
         * Installation ID
         */
        private Long id;

        /**
         * 安装所属账号
         */
        private AccountReqDTO account;

        /**
         * 安装账号请求 DTO
         */
        @Data
        public static class AccountReqDTO {

            /**
             * GitHub 登录名
             */
            private String login;

            /**
             * 账号类型
             */
            private String type;
        }
    }
}

# Dependency Vulnerability (SCA) Report (backend) — 2026-08-15

**Commit:** a62ecce
**Generated:** 2026-08-15T20:30:08Z
**Scope:** backend Maven dependency tree, compile scope (OSV.dev batch query, https://api.osv.dev)

**82 known vulnerabilities across the resolved dependency tree.**

| Package | Vuln count | OSV IDs |
| --- | --- | --- |
| org.springframework.boot:spring-boot:3.5.3 | 1 | GHSA-wwpq-f5c3-7hvx |
| org.apache.logging.log4j:log4j-api:2.24.3 | 1 | GHSA-qv9r-c865-cp47 |
| org.apache.tomcat.embed:tomcat-embed-core:10.1.42 | 19 | GHSA-25xr-qj8w-c4vf, GHSA-563x-q5rq-57qp, GHSA-5m62-pw8w-7w9f, GHSA-5mp6-jrq3-r938, GHSA-9m3c-qcxr-9x87, GHSA-9m89-8frq-c98c, GHSA-fpj8-gq4v-p354, GHSA-fv25-8xcx-gqjc, GHSA-gqp3-2cvr-x8m3, GHSA-gx5v-xp9w-j4cg, GHSA-h6fc-48rj-7qqh, GHSA-hgrr-935x-pq79, GHSA-mgp5-rv84-w37q, GHSA-r29c-68gh-xp6x, GHSA-rv64-5gf8-9qq8, GHSA-vfww-5hm6-hx2j, GHSA-wmwf-9ccg-fff5, GHSA-wr62-c79q-cv37, GHSA-x4m4-345f-5h5g |
| org.springframework:spring-web:6.2.8 | 1 | GHSA-7m2p-62gw-p8qq |
| org.springframework:spring-webmvc:6.2.8 | 12 | GHSA-3chg-m5w7-qfv5, GHSA-4773-3jfm-qmx3, GHSA-6hcq-hmm3-jj3c, GHSA-6p4f-wcwh-5vvm, GHSA-72pg-x5f8-j25j, GHSA-957g-f97v-vppc, GHSA-cjpg-rgq5-fr37, GHSA-h3qp-gqrc-q736, GHSA-mq64-j8f9-9gcj, GHSA-r936-gwx5-v52f, GHSA-wg35-8jpf-2xv3, GHSA-x23c-287f-qqv5 |
| org.springframework:spring-expression:6.2.8 | 3 | GHSA-9f52-rjqv-25qv, GHSA-r5w3-xv2f-j59q, GHSA-wxpp-56q6-5pcg |
| org.springframework.data:spring-data-commons:3.5.1 | 4 | GHSA-5m4m-73w9-8433, GHSA-5vpf-xvv7-c8vh, GHSA-88fw-v6x4-3f58, GHSA-9fw2-h3hf-293r |
| org.springframework.boot:spring-boot-starter-actuator:3.5.3 | 2 | GHSA-8hfc-fq58-r658, GHSA-mgvc-8q2h-5pgc |
| io.micrometer:micrometer-core:1.15.1 | 2 | GHSA-g3pr-3p32-fp23, GHSA-w737-wx49-qj23 |
| org.postgresql:postgresql:42.7.7 | 2 | GHSA-98qh-xjc8-98pq, GHSA-j92g-9f8w-j867 |
| com.fasterxml.jackson.core:jackson-core:2.19.1 | 2 | GHSA-72hv-8253-57qq, GHSA-r7wm-3cxj-wff9 |
| org.apache.commons:commons-lang3:3.17.0 | 1 | GHSA-j288-q9x7-2f5v |
| com.fasterxml.jackson.core:jackson-databind:2.19.1 | 5 | GHSA-3pjw-73gf-8qr5, GHSA-5jmj-h7xm-6q6v, GHSA-hgj6-7826-r7m5, GHSA-j3rv-43j4-c7qm, GHSA-rmj7-2vxq-3g9f |
| io.netty:netty-handler:4.1.122.Final | 3 | GHSA-3qp7-7mw8-wx86, GHSA-c653-97m9-rcg9, GHSA-x4gw-5cx5-pgmh |
| io.netty:netty-codec:4.1.122.Final | 3 | GHSA-3p8m-j85q-pgmj, GHSA-558v-64gr-wgg4, GHSA-mj4r-2hfc-f8p6 |
| org.springframework.data:spring-data-keyvalue:3.5.1 | 1 | GHSA-xg2j-3hj6-pc24 |
| org.springframework.data:spring-data-mongodb:4.5.1 | 2 | GHSA-5whc-4q84-fj73, GHSA-hc43-m36c-8v33 |
| org.springframework.kafka:spring-kafka:3.3.7 | 3 | GHSA-53w6-v7cv-fc9h, GHSA-xq69-5h5v-x9x4, GHSA-xvfq-4q6q-gxx7 |
| org.springframework.retry:spring-retry:2.0.12 | 1 | GHSA-2827-2mxx-j8pv |
| org.apache.kafka:kafka-clients:3.9.1 | 2 | GHSA-5qcv-4rpc-jp93, GHSA-wf66-mphr-4c4r |
| ch.qos.logback:logback-core:1.5.18 | 4 | GHSA-25qh-j22f-pwp8, GHSA-jhq6-gfmj-v8fx, GHSA-p47f-322f-whfh, GHSA-qqpg-mvqg-649v |
| org.springframework.security:spring-security-web:6.5.1 | 3 | GHSA-293q-567p-wmwq, GHSA-mf92-479x-3373, GHSA-x2r2-rvhq-2mqv |
| org.springframework:spring-core:6.2.8 | 2 | GHSA-659m-px2c-25wj, GHSA-jmp9-x22r-554x |
| org.springframework.security:spring-security-core:6.5.1 | 3 | GHSA-8v5q-rhf3-jphm, GHSA-vxf7-qj7q-83fh, GHSA-x2wq-9x2f-fhj7 |

Look up any ID at https://osv.dev/vulnerability/<id> for severity, affected version ranges, and the fixed version — the batch endpoint used here returns IDs only, not full advisory detail, to keep this a fast routine check rather than one HTTP call per finding. A vulnerability in a *test-scope-only* transitive dependency (not shown here — this query is compile-scope only) is lower priority than one reachable at runtime.

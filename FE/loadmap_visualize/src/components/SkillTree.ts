// src/models/SkillTree.ts

import { DAGRE_LAYOUT } from './layoutConfig';
import { Node, Edge } from 'react-flow-renderer';
import dagre from 'dagre';
import { Position } from '@xyflow/react';


// 각 노드의 유형을 정의하는 열거형
export enum NodeType {
    Track = "Track",
    Category = "Category",
    SubCategory = "SubCategory",
    Module = "Module",
    Resource = "Resource",
    File = "File"
}

// 기본 스킬 노드 인터페이스
export interface SkillNodeBase {
    id: string; // 고유 식별자
    name: string; // 노드 이름
    type: NodeType; // 노드 유형
    children?: SkillNode[]; // 자식 노드들
    resources?: string[]; // 리소스 파일들 (필요 시)
}

// 스킬 트리의 각 노드를 표현하는 타입 (데이터베이스용과 React Flow용을 통합)
export type SkillNode = SkillNodeBase;

// 모킹된 스킬 트리 데이터
export const skillTree: SkillNode = {
    id: "1",
    name: "Backend Development Track",
    type: NodeType.Track,
    children: [
        {
            id: "1-1",
            name: "Programming Fundamentals",
            type: NodeType.Category,
            children: [
                {
                    id: "1-1-1",
                    name: "pf1: Computer Science Basics",
                    type: NodeType.SubCategory,
                    children: [
                        {
                            id: "1-1-1-1",
                            name: "Module 1: 컴퓨터의 작동 원리",
                            type: NodeType.Module,
                            children: [
                                { id: "1-1-1-1-1", name: "컴퓨터의 주요 구성 요소 학습.txt", type: NodeType.File },
                                { id: "1-1-1-1-2", name: "이진법과 컴퓨터 연산 구조 이해.txt", type: NodeType.File },
                                { id: "1-1-1-1-3", name: "컴퓨터 데이터 처리 및 저장 방식 학습.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-1-1-2",
                            name: "Module 2: 운영체제와 프로세스 관리",
                            type: NodeType.Module,
                            children: [
                                { id: "1-1-1-2-1", name: "운영체제의 역할과 종류 학습.txt", type: NodeType.File },
                                { id: "1-1-1-2-2", name: "프로세스와 스레드 관리 원리.txt", type: NodeType.File },
                                { id: "1-1-1-2-3", name: "가상 메모리 관리.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-1-1-3",
                            name: "Module 3: 네트워크 기초",
                            type: NodeType.Module,
                            children: [
                                { id: "1-1-1-3-1", name: "네트워크 구조와 프로토콜 학습.txt", type: NodeType.File },
                                { id: "1-1-1-3-2", name: "데이터 송수신 및 네트워크 계층.txt", type: NodeType.File },
                                { id: "1-1-1-3-3", name: "DNS와 라우팅 원리 이해.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-1-1-4",
                            name: "Resources",
                            type: NodeType.Resource,
                            children: [
                                { id: "1-1-1-4-1", name: "computer_science_basics_resources.md", type: NodeType.File }
                            ]
                        }
                    ]
                },
                {
                    id: "1-1-2",
                    name: "pf2: Data Structures and Algorithms",
                    type: NodeType.SubCategory,
                    children: [
                        {
                            id: "1-1-2-1",
                            name: "Module 1: 기초 자료 구조",
                            type: NodeType.Module,
                            children: [
                                { id: "1-1-2-1-1", name: "배열, 스택, 큐, 연결 리스트.txt", type: NodeType.File },
                                { id: "1-1-2-1-2", name: "시간 및 공간 복잡도 분석.txt", type: NodeType.File },
                                { id: "1-1-2-1-3", name: "해시 테이블의 구조 이해.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-1-2-2",
                            name: "Module 2: 트리와 그래프",
                            type: NodeType.Module,
                            children: [
                                { id: "1-1-2-2-1", name: "이진 트리와 탐색 방법.txt", type: NodeType.File },
                                { id: "1-1-2-2-2", name: "그래프 이론과 탐색 알고리즘.txt", type: NodeType.File },
                                { id: "1-1-2-2-3", name: "최단 경로 알고리즘 학습.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-1-2-3",
                            name: "Module 3: 정렬과 검색 알고리즘",
                            type: NodeType.Module,
                            children: [
                                { id: "1-1-2-3-1", name: "다양한 정렬 알고리즘의 동작.txt", type: NodeType.File },
                                { id: "1-1-2-3-2", name: "이진 탐색의 개념 및 구현.txt", type: NodeType.File },
                                { id: "1-1-2-3-3", name: "성능 최적화 기법 학습.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-1-2-4",
                            name: "Resources",
                            type: NodeType.Resource,
                            children: [
                                { id: "1-1-2-4-1", name: "data_structures_and_algorithms_resources.md", type: NodeType.File }
                            ]
                        }
                    ]
                },
                {
                    id: "1-1-3",
                    name: "pf3: Operating Systems Concepts",
                    type: NodeType.SubCategory,
                    children: [
                        {
                            id: "1-1-3-1",
                            name: "Module 1: 운영 체제의 구조",
                            type: NodeType.Module,
                            children: [
                                { id: "1-1-3-1-1", name: "운영체제와 커널의 역할.txt", type: NodeType.File },
                                { id: "1-1-3-1-2", name: "프로세스 관리와 스케줄링 원리.txt", type: NodeType.File },
                                { id: "1-1-3-1-3", name: "메모리 관리 기법 학습.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-1-3-2",
                            name: "Module 2: 입출력 관리와 파일 시스템",
                            type: NodeType.Module,
                            children: [
                                { id: "1-1-3-2-1", name: "입출력 장치의 동작 원리.txt", type: NodeType.File },
                                { id: "1-1-3-2-2", name: "파일 시스템의 구조와 데이터 관리.txt", type: NodeType.File },
                                { id: "1-1-3-2-3", name: "RAID 구조 및 백업 전략.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-1-3-3",
                            name: "Resources",
                            type: NodeType.Resource,
                            children: [
                                { id: "1-1-3-3-1", name: "operating_systems_concepts_resources.md", type: NodeType.File }
                            ]
                        }
                    ]
                }
            ]
        },
        {
            id: "1-2",
            name: "Backend Development",
            type: NodeType.Category,
            children: [
                {
                    id: "1-2-1",
                    name: "be1: Programming Languages",
                    type: NodeType.SubCategory,
                    children: [
                        {
                            id: "1-2-1-1",
                            name: "Node.js (JavaScript)",
                            type: NodeType.Module,
                            children: [
                                {
                                    id: "1-2-1-1-1",
                                    name: "Module 1: Node.js의 기본",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-2-1-1-1-1", name: "Node.js 환경 설정 및 모듈 시스템 이해.txt", type: NodeType.File },
                                        { id: "1-2-1-1-1-2", name: "비동기 프로그래밍 및 async/await 사용.txt", type: NodeType.File },
                                        { id: "1-2-1-1-1-3", name: "HTTP 서버 생성 및 요청/응답 처리 구현.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-2-1-1-2",
                                    name: "Module 2: Express.js를 활용한 REST API",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-2-1-1-2-1", name: "Express.js 구조와 라우팅 학습.txt", type: NodeType.File },
                                        { id: "1-2-1-1-2-2", name: "미들웨어와 API 설계.txt", type: NodeType.File },
                                        { id: "1-2-1-1-2-3", name: "데이터베이스 연동 및 CRUD 구현.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-2-1-1-3",
                                    name: "Resources",
                                    type: NodeType.Resource,
                                    children: [
                                        { id: "1-2-1-1-3-1", name: "nodejs_resources.md", type: NodeType.File }
                                    ]
                                }
                            ]
                        },
                        {
                            id: "1-2-1-2",
                            name: "Python (Django, Flask)",
                            type: NodeType.Module,
                            children: [
                                {
                                    id: "1-2-1-2-1",
                                    name: "Module 1: Django 프레임워크 기본",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-2-1-2-1-1", name: "Django의 구조와 MVC 패턴.txt", type: NodeType.File },
                                        { id: "1-2-1-2-1-2", name: "ORM을 통한 데이터베이스 연동.txt", type: NodeType.File },
                                        { id: "1-2-1-2-1-3", name: "템플릿 시스템으로 동적 페이지 렌더링.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-2-1-2-2",
                                    name: "Module 2: Flask를 활용한 경량 웹 개발",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-2-1-2-2-1", name: "Flask의 라우팅과 뷰 함수.txt", type: NodeType.File },
                                        { id: "1-2-1-2-2-2", name: "RESTful API 개발과 데이터 검증.txt", type: NodeType.File },
                                        { id: "1-2-1-2-2-3", name: "JWT 인증을 통한 사용자 인증 구현.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-2-1-2-3",
                                    name: "Resources",
                                    type: NodeType.Resource,
                                    children: [
                                        { id: "1-2-1-2-3-1", name: "django_flask_resources.md", type: NodeType.File }
                                    ]
                                }
                            ]
                        },
                        {
                            id: "1-2-1-3",
                            name: "Java (Spring Boot)",
                            type: NodeType.Module,
                            children: [
                                {
                                    id: "1-2-1-3-1",
                                    name: "Module 1: Spring Boot 기본",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-2-1-3-1-1", name: "Spring Boot 프로젝트 생성 및 설정.txt", type: NodeType.File },
                                        { id: "1-2-1-3-1-2", name: "REST 컨트롤러 작성 및 테스트.txt", type: NodeType.File },
                                        { id: "1-2-1-3-1-3", name: "스프링 보안 및 인증 구현.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-2-1-3-2",
                                    name: "Resources",
                                    type: NodeType.Resource,
                                    children: [
                                        { id: "1-2-1-3-2-1", name: "spring_boot_resources.md", type: NodeType.File }
                                    ]
                                }
                            ]
                        },
                        {
                            id: "1-2-1-4",
                            name: "Resources",
                            type: NodeType.Resource,
                            children: [
                                { id: "1-2-1-4-1", name: "programming_languages_resources.md", type: NodeType.File }
                            ]
                        }
                    ]
                },
                {
                    id: "1-2-2",
                    name: "be6: APIs and Microservices",
                    type: NodeType.SubCategory,
                    children: [
                        {
                            id: "1-2-2-1",
                            name: "RESTful APIs",
                            type: NodeType.Module,
                            children: [
                                { id: "1-2-2-1-1", name: "REST API 설계 원칙.txt", type: NodeType.File },
                                { id: "1-2-2-1-2", name: "HTTP 메소드와 상태 코드 활용.txt", type: NodeType.File },
                                { id: "1-2-2-1-3", name: "Swagger를 통한 API 문서화.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-2-2-2",
                            name: "GraphQL",
                            type: NodeType.Module,
                            children: [
                                { id: "1-2-2-2-1", name: "GraphQL 스키마 정의 및 쿼리 작성.txt", type: NodeType.File },
                                { id: "1-2-2-2-2", name: "REST와 GraphQL 비교 및 활용.txt", type: NodeType.File },
                                { id: "1-2-2-2-3", name: "Apollo Server를 사용한 GraphQL API 구현.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-2-2-3",
                            name: "gRPC",
                            type: NodeType.Module,
                            children: [
                                { id: "1-2-2-3-1", name: "gRPC의 동작 원리와 활용.txt", type: NodeType.File },
                                { id: "1-2-2-3-2", name: "프로토콜 버퍼를 통한 데이터 직렬화.txt", type: NodeType.File },
                                { id: "1-2-2-3-3", name: "gRPC를 활용한 마이크로서비스 통신 구현.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-2-2-4",
                            name: "Microservices Architecture",
                            type: NodeType.Module,
                            children: [
                                { id: "1-2-2-4-1", name: "마이크로서비스 아키텍처 설계 원리.txt", type: NodeType.File },
                                { id: "1-2-2-4-2", name: "서비스 간 통신 및 API 게이트웨이 구성.txt", type: NodeType.File },
                                { id: "1-2-2-4-3", name: "서비스 분리 및 독립 배포 전략 학습.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-2-2-5",
                            name: "Resources",
                            type: NodeType.Resource,
                            children: [
                                { id: "1-2-2-5-1", name: "apis_and_microservices_resources.md", type: NodeType.File }
                            ]
                        }
                    ]
                },
                {
                    id: "1-2-3",
                    name: "be11: Web Security",
                    type: NodeType.SubCategory,
                    children: [
                        {
                            id: "1-2-3-1",
                            name: "Authentication (OAuth, JWT)",
                            type: NodeType.Module,
                            children: [
                                { id: "1-2-3-1-1", name: "OAuth와 JWT의 개념 및 동작 원리.txt", type: NodeType.File },
                                { id: "1-2-3-1-2", name: "OAuth2.0 인증 과정 및 서버 설정.txt", type: NodeType.File },
                                { id: "1-2-3-1-3", name: "JWT를 활용한 사용자 인증 시스템 구현.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-2-3-2",
                            name: "HTTPS and SSL/TLS",
                            type: NodeType.Module,
                            children: [
                                { id: "1-2-3-2-1", name: "HTTPS와 SSL/TLS의 기본 개념.txt", type: NodeType.File },
                                { id: "1-2-3-2-2", name: "SSL/TLS 인증서 설치 및 구성.txt", type: NodeType.File },
                                { id: "1-2-3-2-3", name: "데이터 암호화 및 전송 보안 실습.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-2-3-3",
                            name: "CSRF, XSS, SQL Injection Protection",
                            type: NodeType.Module,
                            children: [
                                { id: "1-2-3-3-1", name: "주요 보안 취약점과 방어 기법.txt", type: NodeType.File },
                                { id: "1-2-3-3-2", name: "CSRF 공격 방지 방법 (토큰 사용).txt", type: NodeType.File },
                                { id: "1-2-3-3-3", name: "XSS 및 SQL Injection 방어 코딩.txt", type: NodeType.File }
                            ]
                        },
                        {
                            id: "1-2-3-4",
                            name: "Resources",
                            type: NodeType.Resource,
                            children: [
                                { id: "1-2-3-4-1", name: "web_security_resources.md", type: NodeType.File }
                            ]
                        }
                    ]
                },
                {
                    id: "1-2-4",
                    name: "Resources",
                    type: NodeType.Resource,
                    children: [
                        { id: "1-2-4-1", name: "backend_development_resources.md", type: NodeType.File }
                    ]
                }
            ]
        },
        {
            id: "1-3",
            name: "Databases",
            type: NodeType.Track,
            children: [
                {
                    id: "1-3-1",
                    name: "RDBMS",
                    type: NodeType.Category,
                    children: [
                        {
                            id: "1-3-1-1",
                            name: "MySQL",
                            type: NodeType.Module,
                            children: [
                                {
                                    id: "1-3-1-1-1",
                                    name: "Module 1: MySQL 기본",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-3-1-1-1-1", name: "MySQL 설치 및 환경 설정.txt", type: NodeType.File },
                                        { id: "1-3-1-1-1-2", name: "데이터베이스와 테이블 생성.txt", type: NodeType.File },
                                        { id: "1-3-1-1-1-3", name: "SQL을 통한 데이터 조작(CRUD).txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-3-1-1-2",
                                    name: "Module 2: Advanced MySQL",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-3-1-1-2-1", name: "인덱싱과 쿼리 최적화.txt", type: NodeType.File },
                                        { id: "1-3-1-1-2-2", name: "트랜잭션 및 잠금 관리.txt", type: NodeType.File },
                                        { id: "1-3-1-1-2-3", name: "복제 및 백업 전략.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-3-1-1-3",
                                    name: "Resources",
                                    type: NodeType.Resource,
                                    children: [
                                        { id: "1-3-1-1-3-1", name: "mysql_resources.md", type: NodeType.File }
                                    ]
                                }
                            ]
                        },
                        {
                            id: "1-3-1-2",
                            name: "PostgreSQL",
                            type: NodeType.Module,
                            children: [
                                {
                                    id: "1-3-1-2-1",
                                    name: "Module 1: PostgreSQL 기본",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-3-1-2-1-1", name: "PostgreSQL 설치 및 구성.txt", type: NodeType.File },
                                        { id: "1-3-1-2-1-2", name: "데이터 타입 및 테이블 관리.txt", type: NodeType.File },
                                        { id: "1-3-1-2-1-3", name: "고급 쿼리 및 인덱싱 기법.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-3-1-2-2",
                                    name: "Module 2: Advanced PostgreSQL",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-3-1-2-2-1", name: "JSONB 및 배열 데이터 타입 활용.txt", type: NodeType.File },
                                        { id: "1-3-1-2-2-2", name: "트랜잭션 관리 및 동시성 제어.txt", type: NodeType.File },
                                        { id: "1-3-1-2-2-3", name: "고급 데이터베이스 최적화 기법.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-3-1-2-3",
                                    name: "Resources",
                                    type: NodeType.Resource,
                                    children: [
                                        { id: "1-3-1-2-3-1", name: "postgresql_resources.md", type: NodeType.File }
                                    ]
                                }
                            ]
                        },
                        {
                            id: "1-3-1-3",
                            name: "Resources",
                            type: NodeType.Resource,
                            children: [
                                { id: "1-3-1-3-1", name: "rdbms_resources.md", type: NodeType.File }
                            ]
                        }
                    ]
                },
                {
                    id: "1-3-2",
                    name: "NoSQL",
                    type: NodeType.Category,
                    children: [
                        {
                            id: "1-3-2-1",
                            name: "MongoDB",
                            type: NodeType.Module,
                            children: [
                                {
                                    id: "1-3-2-1-1",
                                    name: "Module 1: MongoDB 기본",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-3-2-1-1-1", name: "MongoDB 설치 및 구조 이해.txt", type: NodeType.File },
                                        { id: "1-3-2-1-1-2", name: "CRUD 및 Aggregation Framework 학습.txt", type: NodeType.File },
                                        { id: "1-3-2-1-1-3", name: "인덱스 및 데이터 모델링.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-3-2-1-2",
                                    name: "Module 2: Advanced MongoDB",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-3-2-1-2-1", name: "복제 및 샤딩을 통한 확장성.txt", type: NodeType.File },
                                        { id: "1-3-2-1-2-2", name: "성능 최적화 및 모니터링.txt", type: NodeType.File },
                                        { id: "1-3-2-1-2-3", name: "데이터 보안 및 인증 방법.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-3-2-1-3",
                                    name: "Resources",
                                    type: NodeType.Resource,
                                    children: [
                                        { id: "1-3-2-1-3-1", name: "mongodb_resources.md", type: NodeType.File }
                                    ]
                                }
                            ]
                        },
                        {
                            id: "1-3-2-2",
                            name: "Cassandra",
                            type: NodeType.Module,
                            children: [
                                {
                                    id: "1-3-2-2-1",
                                    name: "Module 1: Cassandra 기본",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-3-2-2-1-1", name: "Cassandra 설치 및 노드 구성.txt", type: NodeType.File },
                                        { id: "1-3-2-2-1-2", name: "CQL을 통한 데이터 관리.txt", type: NodeType.File },
                                        { id: "1-3-2-2-1-3", name: "데이터 분산 및 복제 구조 이해.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-3-2-2-2",
                                    name: "Module 2: Advanced Cassandra",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-3-2-2-2-1", name: "성능 최적화 및 모니터링.txt", type: NodeType.File },
                                        { id: "1-3-2-2-2-2", name: "보안 설정 및 인증 관리.txt", type: NodeType.File },
                                        { id: "1-3-2-2-2-3", name: "고가용성 및 복구 전략.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-3-2-2-3",
                                    name: "Resources",
                                    type: NodeType.Resource,
                                    children: [
                                        { id: "1-3-2-2-3-1", name: "cassandra_resources.md", type: NodeType.File }
                                    ]
                                }
                            ]
                        },
                        {
                            id: "1-3-2-3",
                            name: "Redis",
                            type: NodeType.Module,
                            children: [
                                {
                                    id: "1-3-2-3-1",
                                    name: "Module 1: Redis 기본",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-3-2-3-1-1", name: "Redis 설치 및 구성.txt", type: NodeType.File },
                                        { id: "1-3-2-3-1-2", name: "키-값 데이터 관리 및 명령어.txt", type: NodeType.File },
                                        { id: "1-3-2-3-1-3", name: "데이터 구조 및 TTL 설정 학습.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-3-2-3-2",
                                    name: "Module 2: Advanced Redis",
                                    type: NodeType.Module,
                                    children: [
                                        { id: "1-3-2-3-2-1", name: "클러스터링 및 복제 구성.txt", type: NodeType.File },
                                        { id: "1-3-2-3-2-2", name: "고급 데이터 구조 활용 (해시, 셋, 정렬된 셋).txt", type: NodeType.File },
                                        { id: "1-3-2-3-2-3", name: "성능 튜닝 및 보안.txt", type: NodeType.File }
                                    ]
                                },
                                {
                                    id: "1-3-2-3-3",
                                    name: "Resources",
                                    type: NodeType.Resource,
                                    children: [
                                        { id: "1-3-2-3-3-1", name: "redis_resources.md", type: NodeType.File }
                                    ]
                                }
                            ]
                        },
                        {
                            id: "1-3-2-4",
                            name: "Resources",
                            type: NodeType.Resource,
                            children: [
                                { id: "1-3-2-4-1", name: "nosql_resources.md", type: NodeType.File }
                            ]
                        }
                    ]
                }
            ]
        }
    ]
};

// Helper function to find a node's name by its ID
export const findNodeNameById = (node: SkillNode, id: string): string | undefined => {
    if (node.id === id) return node.name;
    if (node.children) {
        for (const child of node.children) {
            const result = findNodeNameById(child, id);
            if (result) return result;
        }
    }
    console.warn(`Node with ID ${id} not found.`);
    return undefined;
};


// Helper function to get all descendant IDs of a node
export const getAllDescendantIds = (node: SkillNode, id: string): string[] => {
    if (node.id === id) {
        let ids: string[] = [];
        if (node.children) {
            node.children.forEach(child => {
                ids.push(child.id);
                ids = ids.concat(getAllDescendantIds(child, child.id));
            });
        }
        return ids;
    }
    if (node.children) {
        for (const child of node.children) {
            const result = getAllDescendantIds(child, id);
            if (result.length > 0) {
                return result;
            }
        }
    }
    return [];
};

// 트리 구조를 React Flow의 노드와 엣지로 변환하는 유틸리티 함수
export const convertSkillTreeToReactFlow = (
    root: SkillNode,
    expandedNodes: Set<string>
): { nodes: Node[]; edges: Edge[] } => {
    const nodes: Node[] = [];
    const edges: Edge[] = [];
    const nodeNameMap: Map<string, string> = new Map();

    // Dagre 그래프 초기화
    const dagreGraph = new dagre.graphlib.Graph();
    dagreGraph.setGraph(DAGRE_LAYOUT);
    dagreGraph.setDefaultEdgeLabel(() => ({}));

    const nodeWidth = 172;
    const nodeHeight = 36;
    const nodeSourcePosition = Position.Right;
    const nodeTargetPosition = Position.Left;

    dagreGraph.setGraph({ rankdir: 'LR', nodesep: 50, ranksep: 100 });

    const traverse = (node: SkillNode, parentId: string | null = null) => {
        dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight });
        nodeNameMap.set(node.id, node.name);

        if (parentId) {
            dagreGraph.setEdge(parentId, node.id);
            edges.push({
                id: `e${parentId}-${node.id}`,
                source: parentId,
                target: node.id,
                type: 'straight'
            });
        }

        // 자식 노드만 추가 (parent가 확장된 경우)
        if (node.children && expandedNodes.has(node.id)) {
            node.children.forEach(child => traverse(child, node.id));
        }
    };

    traverse(root);

    dagre.layout(dagreGraph);

    dagreGraph.nodes().forEach((v: string) => {
        const nodeWithPosition = dagreGraph.node(v);
        nodes.push({
            id: v,
            type: 'default',
            data: { label: nodeNameMap.get(v) || v },
            position: {
                x: nodeWithPosition.x - nodeWidth / 2,
                y: nodeWithPosition.y - nodeHeight / 2,
            },
            sourcePosition: nodeSourcePosition,
            targetPosition: nodeTargetPosition
        });
    });

    
    return { nodes, edges };
};

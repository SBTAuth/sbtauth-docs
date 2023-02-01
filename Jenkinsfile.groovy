@Library('common-lib') _

def builder = new com.abm.cicd.front.web.build()
def yamlFile = builder.get_web_node_yaml()


pipeline {
  environment {
    DING_DING_ROBOT = 'iotmatrix'

    K8S_SVC_NAME = 'web-sbtauth-docs'
    IMAGE_NAME = 'web-sbtauth-docs'

    PROD_WEB_URL = 'https://docs.sbtauth.io/'
    TEST_WEB_URL = ''

    OUT_PUT_PATH = 'dist'

    BASE_REPO = 'registry.cn-hangzhou.aliyuncs.com/abmatrix'

    BUILD_PATH = '/web-docker/web'
  }

  agent {
    kubernetes {
      yaml yamlFile
    }
  }

  parameters {
    booleanParam(name: 'isFull', defaultValue: false, description: '是否需要清理 node_modules 进行编译')
  }

  stages {

    stage('编译') {
      steps {
        container('node') {
          script {
            builder.build_web_node_yarn_package_custom("yarn --registry https://registry.npmmirror.com", "yarn build && yarn generate")
          }
        }
      }
    }

    stage('构建镜像') {
      environment {
        ABM_REGISTRY = credentials('c5425e91-91d8-4084-8011-82c6497cd40a')
      }
      steps {
        container('image-builder') {
          script {
            builder.build_push_web_package_image()
          }
        }
      }
    }

    stage('部署更新服务') {
      environment {
        K8S_MASTER_IP = credentials('jiaxing-k8s-master-ip')
        K8S_MASTER = credentials('381816aa-abe9-4a66-8842-5f141dff42b4')
      }
      steps {
        container('sshpass') {
          script {
            builder.deploy_web_package_image()
          }
        }
      }
    }
  }

  post {

    success {
      wrap([$class: 'BuildUser']) {
        script {
          builder.post_success_dingding_web_cicd_rst()
        }
      }
    }

    failure {
      wrap([$class: 'BuildUser']) {
        script {
          builder.post_failure_dingding_web_cicd_rst()
        }
      }
    }
  }
}

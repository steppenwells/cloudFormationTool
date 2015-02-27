package model

trait Resource {
  def asCfn: String

  def describe: String

  def resourceType: String
}

class Role(instanceDetails: InstanceDetails) extends Resource {

  val template = s"""
                   "${instanceDetails.resourceAppName}Role": {
                               "Type": "AWS::IAM::Role",
                               "Properties": {
                                   "AssumeRolePolicyDocument": {
                                       "Statement": [ {
                                           "Effect": "Allow",
                                           "Principal": {
                                               "Service": [ "ec2.amazonaws.com" ]
                                           },
                                           "Action": [ "sts:AssumeRole" ]
                                       } ]
                                   },
                                   "Path": "/"
                               }
                           }"""

  override def describe: String = s"instance role: ${instanceDetails.resourceAppName}Role"

  override def asCfn: String = template

  override def resourceType: String = "role"
}

class InstanceProfile(instanceDetails: InstanceDetails) extends Resource {
  override def describe: String = s"instance role: ${instanceDetails.resourceAppName}Role"

  override def asCfn: String = s"""
                                 "${instanceDetails.resourceAppName}InstanceProfile": {
                                             "Type": "AWS::IAM::InstanceProfile",
                                             "Properties": {
                                                 "Path": "/",
                                                 "Roles": [ {"Ref": "${instanceDetails.resourceAppName}Role"} ]
                                             }
                                         }"""

  override def resourceType: String = "instanceProfile"
}

class SSHSecurityGroup() extends Resource {
  override def asCfn: String = s"""
                                 "SSHSecurityGroup": {
                                             "Type": "AWS::EC2::SecurityGroup",
                                             "Properties": {
                                                 "GroupDescription": "Allow SSH access from the office",
                                                 "SecurityGroupIngress": [
                                                     {
                                                         "IpProtocol": "tcp",
                                                         "FromPort": "22",
                                                         "ToPort": "22",
                                                         "CidrIp": {"Ref": "GuardianIP"}
                                                     }
                                                 ]
                                             }
                                         }"""

  override def describe: String = "SSH Security group"

  override def resourceType: String = "securityGroup"
}

class AppServerSecurityGroup(instanceDetails: InstanceDetails) extends Resource {
  override def asCfn: String = s"""
                                 "${instanceDetails.resourceAppName}AppServerSecurityGroup": {
                                             "Type": "AWS::EC2::SecurityGroup",
                                             "Properties": {
                                                 "GroupDescription": "${instanceDetails.appName.get} Application servers",
                                                 "SecurityGroupIngress": [
                                                     {
                                                         "IpProtocol": "tcp",
                                                         "FromPort": 9000,
                                                         "ToPort": 9000,
                                                         "CidrIp": {"Ref": "GuardianIP"}
                                                     },
                                                     {
                                                         "IpProtocol": "tcp",
                                                         "FromPort": 9000,
                                                         "ToPort": 9000,
                                                         "SourceSecurityGroupName": { "Fn::GetAtt": ["${instanceDetails.resourceAppName}LoadBalancer", "SourceSecurityGroup.GroupName"]}
                                                     }
                                                 ]
                                             }
                                         }"""

  override def describe: String = s"server Security Group: ${instanceDetails.resourceAppName}AppServerSecurityGroup"

  override def resourceType: String = "securityGroup"
}

class LoadBalancerSecurityGroup(instanceDetails: InstanceDetails) extends Resource {

  def ingres = instanceDetails.lbType match {
    case Some("httpAndHttps") => """{
                                                           "IpProtocol": "tcp",
                                                           "FromPort": "80",
                                                           "ToPort": "80",
                                                           "CidrIp": "0.0.0.0/0"
                                                       },
                                                       {
                                                           "IpProtocol": "tcp",
                                                           "FromPort": "443",
                                                           "ToPort": "443",
                                                           "CidrIp": "0.0.0.0/0"
                                                       }"""
    case Some("httpOnly") => """{
                                                           "IpProtocol": "tcp",
                                                           "FromPort": "80",
                                                           "ToPort": "80",
                                                           "CidrIp": "0.0.0.0/0"
                                                       }"""
    case Some("httspOnly") => """{
                                                           "IpProtocol": "tcp",
                                                           "FromPort": "443",
                                                           "ToPort": "443",
                                                           "CidrIp": "0.0.0.0/0"
                                                       }"""

    case _ => "{}"
  }

  override def asCfn: String = s"""
                                 "${instanceDetails.resourceAppName}LoadBalancerSecurityGroup": {
                                             "Type": "AWS::EC2::SecurityGroup",
                                             "Properties": {
                                                 "GroupDescription": "${instanceDetails.appName.get} application load balancer",
                                                 "SecurityGroupIngress": [
                                                     $ingres
                                                 ]
                                             }
                                         }"""

  override def describe: String = s"load balancer Security Group: ${instanceDetails.resourceAppName}LoadBalancerSecurityGroup"

  override def resourceType: String = "securityGroup"
}

class LoadBalancer(instanceDetails: InstanceDetails) extends Resource {
  override def asCfn: String = s"""
                                 "${instanceDetails.resourceAppName}LoadBalancer" : {
                                             "Type" : "AWS::ElasticLoadBalancing::LoadBalancer",
                                             "Properties" : {
                                                 "AvailabilityZones" : ["eu-west-1a"],
                                                 "SecurityGroups": [{ "Fn::GetAtt": ["${instanceDetails.resourceAppName}SecurityGroup", "GroupId"] }],
                                                 "Listeners" : [ {
                                                     "LoadBalancerPort" : "80",
                                                     "InstancePort" : "9000",
                                                     "Protocol" : "HTTP"
                                                 } ],

                                                 "HealthCheck" : {
                                                     "Target" : "HTTP:9000/management/healthcheck",
                                                     "HealthyThreshold" : "2",
                                                     "UnhealthyThreshold" : "2",
                                                     "Interval" : "10",
                                                     "Timeout" : "5"
                                                 }
                                             }
                                         }"""

  override def describe: String = s"load balancer: ${instanceDetails.resourceAppName}LoadBalancer"

  override def resourceType: String = "loadBalancer"
}

class AutoScalingGroup(instanceDetails: InstanceDetails, accountDefaults: AccountDefaults) extends Resource {

  lazy val count = instanceDetails.number.getOrElse(1)

  override def asCfn: String = s"""
                                 ${instanceDetails.resourceAppName}AutoscalingGroup":{
                                             "Type":"AWS::AutoScaling::AutoScalingGroup",
                                             "Properties":{
                                                 "AvailabilityZones":["eu-west-1a"],
                                                 "LaunchConfigurationName":{ "Ref":"${instanceDetails.resourceAppName}LaunchConfig" },
                                                 "MinSize":"$count",
                                                 "MaxSize":"${count * 2}",
                                                 "DesiredCapacity": "$count",
                                                 "HealthCheckType" : "ELB",
                                                 "HealthCheckGracePeriod": 300,
                                                 "LoadBalancerNames" : [ { "Ref": "${instanceDetails.resourceAppName}LoadBalancer" }],
                                                 "Tags":[
                                                     {
                                                         "Key":"Stage",
                                                         "Value":{
                                                             "Ref":"Stage"
                                                         },
                                                         "PropagateAtLaunch":"true"
                                                     },
                                                     {
                                                         "Key": "Stack",
                                                         "Value": "${accountDefaults.stack}",
                                                         "PropagateAtLaunch": "true"
                                                     },
                                                     {
                                                         "Key":"App",
                                                         "Value":"${instanceDetails.appName.get}",
                                                         "PropagateAtLaunch":"true"
                                                     }
                                                 ]
                                             }
                                         }"""

  override def describe: String = s"auto scaling group: ${instanceDetails.resourceAppName}AutoscalingGroup"

  override def resourceType: String = "autoScalingGroup"
}

class LaunchConfig(instanceDetails: InstanceDetails, accountDefaults: AccountDefaults) extends Resource {
  override def asCfn: String = s"""
                                "${instanceDetails.resourceAppName}LaunchConfig":{
                                            "Type":"AWS::AutoScaling::LaunchConfiguration",
                                            "Metadata": {
                                                "AWS::CloudFormation::Authentication": {
                                                    "distributionAuthentication": {
                                                        "type": "S3",
                                                        "roleName": { "Ref": "${instanceDetails.resourceAppName}Role" },
                                                        "buckets": [ "${accountDefaults.distBucket}" ]
                                                    }
                                                },
                                                "AWS::CloudFormation::Init": {
                                                    "config": {
                                                        "users": {
                                                            "${accountDefaults.stack}": {
                                                                "homeDir": "/home/${accountDefaults.stack}"
                                                            }
                                                        },
                                                        "files": {
                                                            "/etc/init/${instanceDetails.appName.get}.conf": {
                                                                "source": { "Fn::Join" : ["", [
                                                                    "https://s3-eu-west-1.amazonaws.com/${accountDefaults.distBucket}/${accountDefaults.stack}/",
                                                                    { "Ref": "Stage" },
                                                                    "/${instanceDetails.appName.get}/${instanceDetails.appName.get}.conf"
                                                                ]]},
                                                                "authentication": "distributionAuthentication"
                                                            },
                                                            "/home/${accountDefaults.stack}/${instanceDetails.appName}.zip": {
                                                                "source": { "Fn::Join" : ["", [
                                                                    "https://s3-eu-west-1.amazonaws.com/${accountDefaults.distBucket}/${accountDefaults.stack}/",
                                                                    { "Ref": "Stage" },
                                                                    "/${instanceDetails.appName.get}/${instanceDetails.appName.get}.zip"
                                                                ]]},
                                                                "authentication": "distributionAuthentication"
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            "Properties":{
                                                "KeyName":{ "Ref":"${accountDefaults.key}" },
                                                "ImageId":"${instanceDetails.image.get}",
                                                "SecurityGroups": [
                                                    { "Ref": "${instanceDetails.resourceAppName}AppServerSecurityGroup" },
                                                    { "Ref": "SSHSecurityGroup" }
                                                ],
                                                "InstanceType": "${instanceDetails.instanceType.get}",
                                                "IamInstanceProfile": {"Ref": "${instanceDetails.resourceAppName}InstanceProfile"},
                                                "UserData":{
                                                    "Fn::Base64":{
                                                        "Fn::Join":["", [
                                                            "#!/bin/bash -ev\n",
                                                            "apt-get -y update\n",
                                                            "locale-gen en_GB.UTF-8\n",
                                                            "apt-get -y install openjdk-7-jre-headless\n",
                                                            "apt-get -y install python-setuptools\n",
                                                            "apt-get -y install unzip\n",
                                                            "wget -P /root https://s3.amazonaws.com/cloudformation-examples/aws-cfn-bootstrap-latest.tar.gz","\n",
                                                            "mkdir -p /root/aws-cfn-bootstrap-latest","\n",
                                                            "tar xvfz /root/aws-cfn-bootstrap-latest.tar.gz --strip-components=1 -C /root/aws-cfn-bootstrap-latest","\n",
                                                            "easy_install /root/aws-cfn-bootstrap-latest/","\n",

                                                            "cfn-init -s ", { "Ref" : "AWS::StackId" }, " -r ${instanceDetails.resourceAppName}LaunchConfig ",
                                                            "  --region ", { "Ref" : "AWS::Region" }, " || error_exit 'Failed to run cfn-init'\n",

                                                            "unzip -d /home/${accountDefaults.stack} /home/${accountDefaults.stack}/${instanceDetails.appName.get}\n",

                                                            "mkdir /home/${accountDefaults.stack}/logs\n",

                                                            "chown -R ${accountDefaults.stack} /home/${accountDefaults.stack}\n",
                                                            "chgrp -R ${accountDefaults.stack} /home/${accountDefaults.stack}\n",


                                                            "start ${instanceDetails.appName.get}\n"
                                                        ]]
                                                    }
                                                }
                                            }
                                        }"""

  override def describe: String = s"launch config: ${instanceDetails.resourceAppName}LaunchConfig"

  override def resourceType: String = "launchConfig"
}

class EC2DescribePolicy(instanceDetails: InstanceDetails) extends Resource {
  override def asCfn: String = s"""
        "${instanceDetails.resourceAppName}DescribeEC2Policy" : {
            "Type": "AWS::IAM::Policy",
            "Properties": {
                "PolicyName": "${instanceDetails.resourceAppName}DescribeEC2Policy",
                "PolicyDocument": {
                    "Statement": [ {
                        "Action": ["EC2:Describe*"],
                        "Effect": "Allow",
                        "Resource":"*"
                    }]
                },
                "Roles": [ { "Ref": "${instanceDetails.resourceAppName}Role" } ]
            }
        }
        """

  override def describe: String = s"access policy: ${instanceDetails.resourceAppName}DescribeEC2Policy"

  override def resourceType: String = "policy"
}

class CloudWatchPolicy(instanceDetails: InstanceDetails) extends Resource {
  override def asCfn: String = s"""
        "${instanceDetails.resourceAppName}CloudwatchPolicy" : {
            "Type": "AWS::IAM::Policy",
            "Properties": {
                "PolicyName": "${instanceDetails.resourceAppName}CloudwatchPolicy",
                "PolicyDocument": {
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Action": [ "cloudwatch:*" ],
                            "Resource": "*"
                        }
                    ]
                },
                "Roles": [ { "Ref": "${instanceDetails.resourceAppName}Role" } ]
            }
        }
        """

  override def describe: String = s"access policy: ${instanceDetails.resourceAppName}CloudwatchPolicy"

  override def resourceType: String = "policy"
}

class BucketAccessPolicy(instanceDetails: InstanceDetails, bucketName: String) extends Resource {

  def resourceBucketName = {
    val parts = bucketName.split("-")
    parts.map(capitaliseFirstChar _).mkString("") + "Bucket"
  }

  def capitaliseFirstChar(s: String) = {
    s.head.toUpper + s.tail
  }

  override def asCfn: String = s"""
    "${instanceDetails.resourceAppName}${resourceBucketName}Policy": {
      "Type": "AWS::IAM::Policy",
      "Properties": {
        "PolicyName": "${instanceDetails.resourceAppName}${resourceBucketName}Policy",
        "PolicyDocument": {
          "Statement": [
            {
              "Effect": "Allow",
              "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", s3:ListObject],
              "Resource": ["arn:aws:s3:::$bucketName/*"]
            }
          ]
        },
      "Roles": [{"Ref": "${instanceDetails.resourceAppName}Role"}]
    }"""

  override def describe: String = s"access policy ${instanceDetails.resourceAppName}${resourceBucketName}Policy"

  override def resourceType: String = "policy"
}

class CreatedBucketAccessPolicy(instanceDetails: InstanceDetails, bucketName: String) extends Resource {

  def resourceBucketName = {
    val parts = bucketName.split("-")
    parts.map(capitaliseFirstChar _).mkString("") + "Bucket"
  }

  def capitaliseFirstChar(s: String) = {
    s.head.toUpper + s.tail
  }

  override def asCfn: String = s"""
    "${instanceDetails.resourceAppName}${resourceBucketName}Policy": {
      "Type": "AWS::IAM::Policy",
      "Properties": {
        "PolicyName": "${instanceDetails.resourceAppName}${resourceBucketName}Policy",
        "PolicyDocument": {
          "Statement": [
            {
              "Effect": "Allow",
              "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", s3:ListObject],
              "Resource": [{ "Ref": "${resourceBucketName}" }]
            }
          ]
        },
      "Roles": [{"Ref": "${instanceDetails.resourceAppName}Role"}]
    }"""

  override def describe: String = s"access policy ${instanceDetails.resourceAppName}${resourceBucketName}Policy"

  override def resourceType: String = "policy"
}

class BucketResource(val bucketName: String) extends Resource {

  def resourceBucketName = {
    val parts = bucketName.split("-")
    parts.map(capitaliseFirstChar _).mkString("") + "Bucket"
  }

  def capitaliseFirstChar(s: String) = {
    s.head.toUpper + s.tail
  }

  override def asCfn: String = s"""
      "$resourceBucketName" : {
          "Type" : "AWS::S3::Bucket",
          "Properties" : {
              "AccessControl" : "Private",
              "BucketName" :   {
                  "Fn::Join": [
                      "",
                      [
                          "$bucketName-",
                          { "Fn::FindInMap" :
                            [ "EnvironmentMap",
                              { "Ref" : "Stage" },
                              "lowercase"]
                          }
                      ]
                  ]
              }
          },
          "DeletionPolicy" : "Retain"
      }"""

  override def describe: String = s"bucket: $resourceBucketName"

  override def resourceType: String = "bucket"
}


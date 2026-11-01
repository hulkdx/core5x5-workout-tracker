require "xcodeproj"

# Keep generated build files outside the application checkout.
output = ARGV.fetch(0)
project = Xcodeproj::Project.new(output)
target = project.new_target(:ui_test_bundle, "Core5x5NativeTests", :ios, "18.2")
source = project.main_group.new_file(File.expand_path("RestAlertDeliveryTests.swift", __dir__))
target.source_build_phase.add_file_reference(source)
target.build_configurations.each do |configuration|
  configuration.build_settings["PRODUCT_BUNDLE_IDENTIFIER"] = "com.hulkdx.core5x5.verification.tests"
  configuration.build_settings["GENERATE_INFOPLIST_FILE"] = "YES"
  configuration.build_settings["SWIFT_VERSION"] = "5.0"
  configuration.build_settings["CODE_SIGNING_ALLOWED"] = "NO"
  configuration.build_settings["TARGETED_DEVICE_FAMILY"] = "1,2"
end
project.save
scheme = Xcodeproj::XCScheme.new
scheme.add_build_target(target)
scheme.add_test_target(target)
scheme.save_as(project.path, "Core5x5NativeTests")

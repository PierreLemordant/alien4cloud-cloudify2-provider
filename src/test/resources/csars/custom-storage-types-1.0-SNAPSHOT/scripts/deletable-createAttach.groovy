import org.cloudifysource.utilitydomain.context.ServiceContextFactory

def context = ServiceContextFactory.getServiceContext()

def device = "/dev/vdb"
println "deletable-createAttach.groovy: Creating a volume using the storage template <${storageTemplate}>..."
def createdVolumeId = context.storage.createVolume(storageTemplate)
println "deletable-createAttach.groovy: Creating a volume using the storage template <${storageTemplate}>..."
println "deletable-createAttach.groovy: attaching storage volume <${createdVolumeId}> to <${device}>... "
context.storage.attachVolume(createdVolumeId, device)

return [volumeId: createdVolumeId, device:device]
